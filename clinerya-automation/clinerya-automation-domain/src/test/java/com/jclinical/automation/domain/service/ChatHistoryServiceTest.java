package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.ChatAccess;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatMessage.Direction;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.ports.out.ChatAccessLogPort;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.service.ChannelSettingsServiceTest.InMemorySettings;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakePatients;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entrega 5.5 (D9): el hilo de WhatsApp de cada celular queda guardado; lo ven medicos y recepcion,
 * cada lectura queda auditada y solo el administrador consulta la auditoria. Los mensajes se borran
 * al cumplir la retencion de la clinica (12 meses por defecto).
 */
class ChatHistoryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 12, 0);
    private static final String PHONE = "5215512345678";

    private final UUID clinicId = UUID.randomUUID();
    private final UUID doctorUserId = UUID.randomUUID();
    private final UUID adminUserId = UUID.randomUUID();
    private final UUID strangerUserId = UUID.randomUUID();

    private final InMemoryHistory history = new InMemoryHistory();
    private final InMemoryAccessLog accessLog = new InMemoryAccessLog();
    private final FakePatients patients = new FakePatients();
    private final InMemorySettings settings = new InMemorySettings();
    private final Map<UUID, Set<StaffPermission>> permissions = new HashMap<>();

    private ChatHistoryService service;

    @BeforeEach
    void setUp() {
        permissions.put(doctorUserId, Set.of(StaffPermission.VIEW_PATIENTS));
        permissions.put(adminUserId, Set.of(StaffPermission.VIEW_PATIENTS, StaffPermission.MANAGE_CLINIC));
        StaffPermissionCheckerPort checker =
                (clinic, user, permission) -> permissions.getOrDefault(user, Set.of()).contains(permission);
        patients.add(PHONE, UUID.randomUUID(), "Ana López");
        service = new ChatHistoryService(history, accessLog, patients, settings, checker,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    void outgoingRepliesAreKeptInTheHistoryAsThePatientSawThem() {
        List<PatientNotification> queued = new ArrayList<>();
        RecordingOutboundQueue queue = new RecordingOutboundQueue(queued::add, history,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        PatientNotification reply = new PatientNotification(clinicId, PHONE, new OutboundReply("¿Qué deseas hacer?",
                List.of(new ConversationOption("action:book", "Agendar una cita"))));

        queue.enqueue(reply);

        assertEquals(List.of(reply), queued, "se sigue encolando para enviarse");
        ChatMessage kept = history.messages.get(0);
        assertEquals(Direction.OUTBOUND, kept.direction());
        assertEquals("¿Qué deseas hacer?", kept.text());
        assertEquals(List.of("Agendar una cita"), kept.optionLabels());
        assertEquals(NOW, kept.at());
    }

    @Test
    void doctorsAndReceptionSeeTheChatListWithoutContent() {
        history.record(message(Direction.INBOUND, "Hola", NOW.minusMinutes(3)));

        List<ChatSummary> chats = service.listChats(doctorUserId, clinicId);

        assertEquals(1, chats.size());
        assertEquals(PHONE, chats.get(0).phone());
        assertEquals(List.of("Ana López"), chats.get(0).patientNames());
        assertTrue(accessLog.entries.isEmpty(), "ver la lista no es leer un chat");
        assertThrows(ClinicAccessDeniedException.class, () -> service.listChats(strangerUserId, clinicId));
    }

    @Test
    void readingAChatIsAuditedWithWhoAndWhen() {
        history.record(message(Direction.INBOUND, "Hola", NOW.minusMinutes(3)));
        history.record(message(Direction.OUTBOUND, "Hola, Ana. ¿Qué deseas hacer?", NOW.minusMinutes(2)));

        List<ChatMessage> messages = service.readMessages(doctorUserId, clinicId, PHONE, null, null);

        assertEquals(List.of("Hola, Ana. ¿Qué deseas hacer?", "Hola"), messages.stream().map(ChatMessage::text).toList());
        assertEquals(1, accessLog.entries.size());
        ChatAccess access = accessLog.entries.get(0);
        assertEquals(doctorUserId, access.userId());
        assertEquals(PHONE, access.phone());
        assertEquals(NOW, access.accessedAt());
    }

    @Test
    void someoneWithoutPermissionCannotReadAndNothingIsLogged() {
        assertThrows(ClinicAccessDeniedException.class, () -> service.readMessages(strangerUserId, clinicId, PHONE, null, null));
        assertTrue(accessLog.entries.isEmpty());
    }

    @Test
    void messagesComeInPagesGoingBackInTime() {
        for (int i = 0; i < 5; i++) {
            history.record(message(Direction.INBOUND, "m" + i, NOW.minusMinutes(10 - i)));
        }

        List<ChatMessage> firstPage = service.readMessages(doctorUserId, clinicId, PHONE, null, 2);
        List<ChatMessage> secondPage = service.readMessages(doctorUserId, clinicId, PHONE, firstPage.get(1).at(), 2);

        assertEquals(List.of("m4", "m3"), firstPage.stream().map(ChatMessage::text).toList());
        assertEquals(List.of("m2", "m1"), secondPage.stream().map(ChatMessage::text).toList());
        service.readMessages(doctorUserId, clinicId, PHONE, null, 1000);
        assertEquals(ChatHistoryService.MAX_PAGE, history.lastLimit, "nunca mas de una pagina maxima");
    }

    @Test
    void onlyTheAdministratorSeesWhoReadWhichChat() {
        service.readMessages(doctorUserId, clinicId, PHONE, null, null);

        List<ChatAccess> log = service.accessLog(adminUserId, clinicId, PHONE, null, null, null);

        assertEquals(1, log.size());
        assertEquals(doctorUserId, log.get(0).userId());
        assertThrows(ClinicAccessDeniedException.class, () -> service.accessLog(doctorUserId, clinicId, null, null, null, null));
    }

    @Test
    void messagesOlderThanEachClinicsRetentionArePurged() {
        UUID otherClinic = UUID.randomUUID();
        settings.save(ChannelSettings.unconfigured(clinicId));
        settings.save(ChannelSettings.unconfigured(otherClinic).toBuilder().chatRetentionMonths(3).build());

        service.purgeExpired();

        assertEquals(NOW.minusMonths(12), history.cutoffs.get(clinicId));
        assertEquals(NOW.minusMonths(3), history.cutoffs.get(otherClinic));
    }

    private ChatMessage message(Direction direction, String text, LocalDateTime at) {
        return new ChatMessage(UUID.randomUUID(), clinicId, PHONE, direction, text, List.of(), at);
    }

    static final class InMemoryHistory implements ChatHistoryPort {
        final List<ChatMessage> messages = new ArrayList<>();
        final Map<UUID, LocalDateTime> cutoffs = new HashMap<>();
        int lastLimit;

        @Override
        public void record(ChatMessage message) {
            messages.add(message);
        }

        @Override
        public List<ChatSummary> findChats(UUID clinicId, int limit) {
            return messages.stream().filter(m -> m.clinicId().equals(clinicId)).map(ChatMessage::phone).distinct()
                    .map(phone -> new ChatSummary(phone, List.of(), messages.stream()
                            .filter(m -> m.phone().equals(phone)).map(ChatMessage::at).max(Comparator.naturalOrder()).orElse(null),
                            (int) messages.stream().filter(m -> m.phone().equals(phone)).count()))
                    .toList();
        }

        @Override
        public List<ChatMessage> findMessages(UUID clinicId, String phone, LocalDateTime before, int limit) {
            lastLimit = limit;
            return messages.stream()
                    .filter(m -> m.clinicId().equals(clinicId) && m.phone().equals(phone))
                    .filter(m -> before == null || m.at().isBefore(before))
                    .sorted(Comparator.comparing(ChatMessage::at).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public int deleteOlderThan(UUID clinicId, LocalDateTime cutoff) {
            cutoffs.put(clinicId, cutoff);
            return 0;
        }
    }

    static final class InMemoryAccessLog implements ChatAccessLogPort {
        final List<ChatAccess> entries = new ArrayList<>();

        @Override
        public void record(ChatAccess access) {
            entries.add(access);
        }

        @Override
        public List<ChatAccess> find(UUID clinicId, String phone, UUID userId, LocalDateTime from, LocalDateTime to, int limit) {
            return entries.stream()
                    .filter(e -> e.clinicId().equals(clinicId))
                    .filter(e -> phone == null || e.phone().equals(phone))
                    .filter(e -> userId == null || e.userId().equals(userId))
                    .toList();
        }
    }
}
