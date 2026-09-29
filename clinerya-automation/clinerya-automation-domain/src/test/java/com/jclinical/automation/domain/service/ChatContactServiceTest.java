package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChatContact;
import com.jclinical.automation.domain.model.ChatContact.ContactPatient;
import com.jclinical.automation.domain.model.ChatContact.Signal;
import com.jclinical.automation.domain.ports.out.ChatContactsPort;
import com.jclinical.automation.domain.ports.out.ChatContactsPort.ChatFacts;
import com.jclinical.automation.domain.ports.out.PatientSnapshotPort.PatientSnapshot;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakeDoctors;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ficha breve del contacto en Chats: quien es el numero que escribe (paciente o no), para reconocerlo
 * o detectar spam. Solo datos de identificacion y de citas; nada del expediente.
 */
class ChatContactServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 29, 12, 0);
    private static final String MEXICAN = "5215512345678";
    private static final String FOREIGN = "13055550142";

    private final UUID clinicId = UUID.randomUUID();
    private final UUID receptionistId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final InMemoryContacts contacts = new InMemoryContacts();
    private final Map<String, List<PatientSnapshot>> patientsByPhone = new HashMap<>();
    private final FakeDoctors doctors = new FakeDoctors();

    private ChatContactService service;

    @BeforeEach
    void setUp() {
        doctors.add(doctorId, "Dra. Ramírez");
        service = new ChatContactService(contacts, (clinic, phone) -> patientsByPhone.getOrDefault(phone, List.of()), doctors,
                (clinic, user, permission) -> user.equals(receptionistId) && permission == StaffPermission.VIEW_PATIENTS,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    void aRegisteredPatientIsRecognizedByAgeVisitsAndConsent() {
        contacts.facts.put(MEXICAN, new ChatFacts(NOW.minusMonths(9), "Hola, ¿tienen lugar?", 6, "Ana"));
        UUID patientId = UUID.randomUUID();
        patientsByPhone.put(MEXICAN, List.of(new PatientSnapshot(patientId, "Ana López García", LocalDate.of(1992, 3, 10),
                LocalDateTime.of(2025, 3, 1, 9, 0), true, NOW.plusDays(3), doctorId, NOW.minusDays(48), doctorId, 7, 1, 0)));

        ChatContact contact = service.contact(receptionistId, clinicId, MEXICAN);

        ContactPatient ana = contact.patients().getFirst();
        assertEquals("Ana López García", ana.fullName());
        assertEquals(34, ana.age());
        assertTrue(ana.whatsappConsent());
        assertEquals("Dra. Ramírez", ana.nextDoctorName());
        assertEquals("Dra. Ramírez", ana.lastAttendedDoctorName());
        assertEquals(7, ana.attended());
        assertEquals(6, contact.messageCount());
        assertEquals(NOW.minusMonths(9), contact.firstMessageAt());
        assertTrue(contact.signals().isEmpty(), "un paciente conocido de Mexico no levanta alertas");
    }

    @Test
    void anUnknownForeignNumberWhoseFirstMessageHasALinkRaisesEveryAlert() {
        contacts.facts.put(FOREIGN, new ChatFacts(NOW.minusHours(3), "Gana dinero desde casa https://promo.example", 1,
                "Promociones MX"));

        ChatContact contact = service.contact(receptionistId, clinicId, FOREIGN);

        assertTrue(contact.patients().isEmpty());
        assertEquals("Promociones MX", contact.profileName());
        assertEquals(List.of(Signal.FOREIGN_NUMBER, Signal.FIRST_MESSAGE_HAS_LINK, Signal.NEVER_HAD_APPOINTMENT),
                contact.signals());
    }

    @Test
    void aPatientWithoutAnyAppointmentStillShowsThatItNeverHadOne() {
        contacts.facts.put(MEXICAN, new ChatFacts(NOW.minusDays(1), "Hola", 2, null));
        patientsByPhone.put(MEXICAN, List.of(new PatientSnapshot(UUID.randomUUID(), "Luis Pérez", null, NOW.minusDays(1),
                false, null, null, null, null, 0, 0, 0)));

        ChatContact contact = service.contact(receptionistId, clinicId, MEXICAN);

        assertEquals(List.of(Signal.NEVER_HAD_APPOINTMENT), contact.signals());
        assertEquals(null, contact.patients().getFirst().age(), "sin fecha de nacimiento no se inventa la edad");
    }

    @Test
    void wwwLinksAlsoCountAsLinks() {
        contacts.facts.put(MEXICAN, new ChatFacts(NOW, "visita www.ofertas.mx", 1, null));

        assertTrue(service.contact(receptionistId, clinicId, MEXICAN).signals().contains(Signal.FIRST_MESSAGE_HAS_LINK));
    }

    @Test
    void aNumberThatNeverWroteHasNoFacts() {
        ChatContact contact = service.contact(receptionistId, clinicId, MEXICAN);

        assertEquals(0, contact.messageCount());
        assertFalse(contact.signals().contains(Signal.FIRST_MESSAGE_HAS_LINK));
    }

    @Test
    void theChatListShowsTheWhatsAppProfileNameOfEachNumber() {
        ChatHistoryServiceTest.InMemoryHistory history = new ChatHistoryServiceTest.InMemoryHistory();
        history.record(new com.jclinical.automation.domain.model.ChatMessage(UUID.randomUUID(), clinicId, FOREIGN,
                com.jclinical.automation.domain.model.ChatMessage.Direction.INBOUND, "Hola", List.of(), NOW));
        contacts.saveProfileName(clinicId, FOREIGN, "Promociones MX", NOW);
        ChatHistoryService chats = new ChatHistoryService(history, null, new ConversationServiceTest.FakePatients(),
                new ChannelSettingsServiceTest.InMemorySettings(), (clinic, user, permission) -> true,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC), null, contacts);

        assertEquals("Promociones MX", chats.listChats(receptionistId, clinicId).getFirst().profileName());
    }

    @Test
    void onlyStaffWhoCanReadChatsSeeTheContact() {
        assertThrows(ClinicAccessDeniedException.class, () -> service.contact(UUID.randomUUID(), clinicId, MEXICAN));
    }

    static final class InMemoryContacts implements ChatContactsPort {
        final Map<String, ChatFacts> facts = new HashMap<>();

        @Override
        public void saveProfileName(UUID clinicId, String phone, String profileName, LocalDateTime at) {
            ChatFacts current = facts.getOrDefault(phone, new ChatFacts(at, null, 0, null));
            facts.put(phone, new ChatFacts(current.firstMessageAt(), current.firstInboundText(), current.messageCount(),
                    profileName));
        }

        @Override
        public Map<String, String> profileNames(UUID clinicId, Collection<String> phones) {
            return phones.stream().filter(phone -> facts.containsKey(phone) && facts.get(phone).profileName() != null)
                    .collect(Collectors.toMap(phone -> phone, phone -> facts.get(phone).profileName()));
        }

        @Override
        public Optional<ChatFacts> facts(UUID clinicId, String phone) {
            return Optional.ofNullable(facts.get(phone));
        }
    }
}
