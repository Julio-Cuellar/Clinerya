package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.IntentInterpreterPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CU-2 (paciente registrado solicita cita) sobre la maquina de estados. La garantia central: toda
 * transicion sale de una opcion que el propio motor ofrecio. El texto libre solo se traduce a una de
 * esas opciones; nada que el interprete invente puede mover la conversacion.
 */
class ConversationServiceTest {

    private static final String PHONE = "5215512345678";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 24, 10, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID lastDoctorId = UUID.randomUUID();
    private final UUID otherDoctorId = UUID.randomUUID();

    private final FakePatients patients = new FakePatients();
    private final FakeDoctors doctors = new FakeDoctors();
    private final FakeSlots slots = new FakeSlots();
    private final FakeRequests requests = new FakeRequests();
    private final FakeInterpreter interpreter = new FakeInterpreter();
    private final InMemoryConversations conversations = new InMemoryConversations();
    private final MutableClock clock = new MutableClock(NOW);

    private ConversationService service;

    @BeforeEach
    void setUp() {
        service = new ConversationService(conversations, patients, doctors, slots, requests, interpreter, clock);
        patients.add(PHONE, patientId, "Ana López");
        doctors.add(lastDoctorId, "Dra. Beatriz Ramos");
        doctors.add(otherDoctorId, "Dr. Carlos Díaz");
        doctors.lastDoctor.put(patientId, lastDoctorId);
        slots.add(lastDoctorId, NOW.plusDays(1).withHour(10), NOW.plusDays(1).withHour(11), NOW.plusDays(2).withHour(16));
        slots.add(otherDoctorId, NOW.plusDays(3).withHour(9));
    }

    @Test
    void aKnownPatientIsGreetedWithTheMenu() {
        OutboundReply reply = send("Hola");

        assertEquals(ConversationState.MENU, current().state());
        assertTrue(reply.text().contains("Ana"), reply.text());
        assertTrue(optionIds(reply).contains(ConversationService.BOOK), "el menu ofrece agendar");
    }

    @Test
    void aSharedPhoneAsksWhichPatientTheAppointmentIsFor() {
        UUID childId = UUID.randomUUID();
        patients.add(PHONE, childId, "Luis López");

        OutboundReply reply = send("Hola");

        assertEquals(ConversationState.ELEGIR_PACIENTE, current().state());
        assertEquals(2, reply.options().size());

        send(null, "patient:" + childId);

        assertEquals(ConversationState.MENU, current().state());
        assertEquals(childId, current().patientId());
    }

    @Test
    void anUnknownPhoneIsNotBookedAndTheConversationCloses() {
        OutboundReply reply = send("5500000000", "Hola", null);

        assertEquals(ConversationState.CERRADA, conversations.lastSaved.state());
        assertTrue(reply.options().isEmpty());
        assertTrue(requests.submitted.isEmpty(), "nunca se agenda a un paciente no registrado");
    }

    @Test
    void bookingOffersTheLastDoctorOrTheFullList() {
        send("Hola");

        OutboundReply reply = send(null, ConversationService.BOOK);

        assertEquals(ConversationState.ELEGIR_MEDICO, current().state());
        assertEquals(List.of(ConversationService.LAST_DOCTOR, ConversationService.SHOW_DOCTORS), optionIds(reply));
        assertTrue(reply.options().get(0).label().contains("Beatriz"), reply.options().get(0).label());
    }

    @Test
    void withoutAPreviousDoctorTheListIsShownDirectly() {
        doctors.lastDoctor.clear();
        send("Hola");

        OutboundReply reply = send(null, ConversationService.BOOK);

        assertEquals(ConversationState.ELEGIR_MEDICO, current().state());
        assertEquals(List.of("doctor:" + lastDoctorId, "doctor:" + otherDoctorId), optionIds(reply));
    }

    @Test
    void choosingTheLastDoctorListsOnlyThatDoctorsSlots() {
        send("Hola");
        send(null, ConversationService.BOOK);

        OutboundReply reply = send(null, ConversationService.LAST_DOCTOR);

        assertEquals(ConversationState.ELEGIR_CUPO, current().state());
        assertEquals(lastDoctorId, current().doctorStaffId());
        assertEquals(3, reply.options().size());
        assertTrue(reply.options().stream().allMatch(option -> option.id().startsWith("slot:")));
    }

    @Test
    void choosingFromTheDoctorListListsThatDoctorsSlots() {
        send("Hola");
        send(null, ConversationService.BOOK);
        send(null, ConversationService.SHOW_DOCTORS);

        OutboundReply reply = send(null, "doctor:" + otherDoctorId);

        assertEquals(otherDoctorId, current().doctorStaffId());
        assertEquals(1, reply.options().size());
    }

    @Test
    void aDoctorWithoutSlotsSendsThePatientBackToChooseAnother() {
        slots.byDoctor.remove(otherDoctorId);
        send("Hola");
        send(null, ConversationService.BOOK);
        send(null, ConversationService.SHOW_DOCTORS);

        OutboundReply reply = send(null, "doctor:" + otherDoctorId);

        assertEquals(ConversationState.ELEGIR_MEDICO, current().state());
        assertTrue(reply.text().contains("no tiene horarios"), reply.text());
    }

    @Test
    void choosingASlotSendsTheRequestToThatDoctorAndWaits() {
        send("Hola");
        send(null, ConversationService.BOOK);
        OutboundReply slotList = send(null, ConversationService.LAST_DOCTOR);
        String firstSlot = slotList.options().get(0).id();

        OutboundReply reply = send(null, firstSlot);

        assertEquals(ConversationState.ESPERANDO_MEDICO, current().state());
        assertEquals(1, requests.submitted.size());
        AppointmentRequestPort.NewAppointmentRequest request = requests.submitted.get(0);
        assertEquals(patientId, request.patientId());
        assertEquals(lastDoctorId, request.doctorStaffId());
        assertEquals(NOW.plusDays(1).withHour(10), request.start());
        assertEquals(PHONE, request.patientPhone());
        assertTrue(reply.text().contains("Beatriz"), reply.text());
    }

    @Test
    void aSlotTakenMeanwhileIsReportedAndTheListIsOfferedAgain() {
        send("Hola");
        send(null, ConversationService.BOOK);
        OutboundReply slotList = send(null, ConversationService.LAST_DOCTOR);
        requests.rejectNext = true;

        OutboundReply reply = send(null, slotList.options().get(0).id());

        assertEquals(ConversationState.ELEGIR_CUPO, current().state());
        assertTrue(reply.text().contains("ya no está disponible"), reply.text());
        assertFalse(reply.options().isEmpty());
    }

    @Test
    void anOptionThatWasNotOfferedNeverMovesTheConversation() {
        send("Hola");
        send(null, ConversationService.BOOK);

        OutboundReply reply = send(null, "slot:2026-12-24T10:00|2026-12-24T10:45");

        assertEquals(ConversationState.ELEGIR_MEDICO, current().state());
        assertTrue(requests.submitted.isEmpty());
        assertFalse(reply.options().isEmpty(), "se vuelven a ofrecer las opciones validas");
    }

    @Test
    void freeTextIsTranslatedIntoOneOfTheOfferedOptions() {
        send("Hola");
        interpreter.answer = ConversationService.BOOK;

        send("quiero sacar una cita por favor");

        assertEquals(ConversationState.ELEGIR_MEDICO, current().state());
        assertTrue(interpreter.lastOptions.stream().anyMatch(option -> option.id().equals(ConversationService.BOOK)),
                "el interprete solo recibe las opciones ofrecidas");
    }

    @Test
    void anInterpreterAnswerOutsideTheOfferedOptionsIsIgnored() {
        send("Hola");
        interpreter.answer = "slot:2026-12-24T10:00|2026-12-24T10:45";

        send("agéndame mañana a las 10 con quien sea");

        assertEquals(ConversationState.MENU, current().state());
        assertTrue(requests.submitted.isEmpty());
    }

    @Test
    void anInterpreterFailureRepromptsInsteadOfBreaking() {
        send("Hola");
        interpreter.fail = true;

        OutboundReply reply = send("quiero una cita");

        assertEquals(ConversationState.MENU, current().state());
        assertFalse(reply.options().isEmpty());
    }

    @Test
    void afterThreeUnrecognizedMessagesThePatientIsPointedToTheClinic() {
        send("Hola");
        interpreter.answer = null;

        send("asdf");
        send("qwerty");
        OutboundReply third = send("???");

        assertTrue(third.text().contains("comunícate con la clínica"), third.text());
        assertEquals(ConversationState.MENU, current().state());
    }

    @Test
    void theMenuKeywordRestartsFromAnyStep() {
        send("Hola");
        send(null, ConversationService.BOOK);
        send(null, ConversationService.LAST_DOCTOR);

        send("menú");

        assertEquals(ConversationState.MENU, current().state());
        assertEquals(null, current().doctorStaffId(), "reiniciar descarta lo elegido");
    }

    @Test
    void whileWaitingForTheDoctorThePatientIsToldTheRequestIsPending() {
        send("Hola");
        send(null, ConversationService.BOOK);
        OutboundReply slotList = send(null, ConversationService.LAST_DOCTOR);
        send(null, slotList.options().get(0).id());

        OutboundReply reply = send("¿ya me confirmaron?");

        assertEquals(ConversationState.ESPERANDO_MEDICO, current().state());
        assertTrue(reply.text().contains("pendiente"), reply.text());
        assertEquals(1, requests.submitted.size(), "no se duplica la solicitud");
    }

    @Test
    void anIdleConversationExpiresAndTheNextMessageStartsOver() {
        send("Hola");
        send(null, ConversationService.BOOK);
        clock.advance(Duration.ofMinutes(31));

        send("Hola");

        assertEquals(ConversationState.MENU, current().state());
        assertEquals(2, conversations.all.size(), "la conversacion vencida no se reutiliza");
        assertEquals(ConversationState.EXPIRADA, conversations.all.get(0).state());
    }

    // ---- utilidades -------------------------------------------------------------------------

    private OutboundReply send(String text) {
        return send(PHONE, text, null);
    }

    private OutboundReply send(String text, String optionId) {
        return send(PHONE, text, optionId);
    }

    private OutboundReply send(String phone, String text, String optionId) {
        List<OutboundReply> replies = service.handle(new InboundMessage(clinicId, phone, text, optionId, clock.now()));
        assertEquals(1, replies.size());
        return replies.get(0);
    }

    private Conversation current() {
        return conversations.findActive(clinicId, PHONE).orElseThrow();
    }

    private static List<String> optionIds(OutboundReply reply) {
        return reply.options().stream().map(ConversationOption::id).toList();
    }

    // ---- dobles de prueba ---------------------------------------------------------------------

    static final class FakePatients implements PatientDirectoryPort {
        final Map<String, List<PatientContact>> byPhone = new HashMap<>();

        void add(String phone, UUID id, String name) {
            byPhone.computeIfAbsent(phone, key -> new ArrayList<>()).add(new PatientContact(id, name));
        }

        @Override
        public List<PatientContact> findByPhone(UUID clinicId, String phone) {
            return byPhone.getOrDefault(phone, List.of());
        }
    }

    static final class FakeDoctors implements DoctorDirectoryPort {
        final List<DoctorContact> all = new ArrayList<>();
        final Map<UUID, UUID> lastDoctor = new HashMap<>();

        void add(UUID id, String name) {
            all.add(new DoctorContact(id, name));
        }

        @Override
        public List<DoctorContact> listDoctors(UUID clinicId) {
            return all;
        }

        @Override
        public Optional<DoctorContact> lastDoctorOf(UUID clinicId, UUID patientId) {
            UUID id = lastDoctor.get(patientId);
            return all.stream().filter(doctor -> doctor.staffId().equals(id)).findFirst();
        }
    }

    static final class FakeSlots implements SlotAvailabilityPort {
        final Map<UUID, List<AvailableSlot>> byDoctor = new HashMap<>();

        void add(UUID doctorId, LocalDateTime... starts) {
            List<AvailableSlot> list = byDoctor.computeIfAbsent(doctorId, key -> new ArrayList<>());
            for (LocalDateTime start : starts) {
                list.add(new AvailableSlot(start, start.plusMinutes(45)));
            }
        }

        @Override
        public List<AvailableSlot> availableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit) {
            return byDoctor.getOrDefault(doctorStaffId, List.of()).stream().limit(limit).toList();
        }
    }

    static final class FakeRequests implements AppointmentRequestPort {
        final List<NewAppointmentRequest> submitted = new ArrayList<>();
        final List<AvailableSlot> chosen = new ArrayList<>();
        final List<UUID> chosenRequests = new ArrayList<>();
        final List<UUID> declined = new ArrayList<>();
        boolean rejectNext;
        boolean failChoice;

        @Override
        public UUID submit(NewAppointmentRequest request) {
            if (rejectNext) {
                rejectNext = false;
                throw new SlotNoLongerAvailableException();
            }
            submitted.add(request);
            return UUID.randomUUID();
        }

        @Override
        public UUID chooseOption(UUID clinicId, UUID requestId, LocalDateTime start, LocalDateTime end) {
            if (failChoice) {
                throw new SlotNoLongerAvailableException();
            }
            chosen.add(new AvailableSlot(start, end));
            chosenRequests.add(requestId);
            return UUID.randomUUID();
        }

        @Override
        public void declineOptions(UUID clinicId, UUID requestId) {
            declined.add(requestId);
        }
    }

    static final class FakeInterpreter implements IntentInterpreterPort {
        String answer;
        boolean fail;
        List<ConversationOption> lastOptions = List.of();

        @Override
        public Optional<String> interpret(String text, List<ConversationOption> options) {
            lastOptions = options;
            if (fail) {
                throw new IllegalStateException("Gemini no respondio");
            }
            return Optional.ofNullable(answer);
        }
    }

    static final class InMemoryConversations implements ConversationRepositoryPort {
        final List<Conversation> all = new ArrayList<>();
        Conversation lastSaved;

        @Override
        public Optional<Conversation> findActive(UUID clinicId, String phone) {
            return all.stream()
                    .filter(conversation -> conversation.clinicId().equals(clinicId) && conversation.phone().equals(phone))
                    .filter(conversation -> !conversation.state().isTerminal())
                    .findFirst();
        }

        @Override
        public Optional<Conversation> findById(UUID conversationId) {
            return all.stream().filter(conversation -> conversation.id().equals(conversationId)).findFirst();
        }

        @Override
        public Conversation save(Conversation conversation) {
            all.removeIf(existing -> existing.id().equals(conversation.id()));
            all.add(conversation);
            all.sort((left, right) -> left.createdAt().compareTo(right.createdAt()));
            lastSaved = conversation;
            return conversation;
        }
    }

    static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(LocalDateTime start) {
            this.instant = start.toInstant(ZoneOffset.UTC);
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        LocalDateTime now() {
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
