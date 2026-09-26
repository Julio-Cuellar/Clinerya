package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent;
import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent.Outcome;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakeDoctors;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakeInterpreter;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakePatients;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakeRequests;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakeSlots;
import com.jclinical.automation.domain.service.ConversationServiceTest.InMemoryConversations;
import com.jclinical.automation.domain.service.ConversationServiceTest.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entrega 4 (CU-3, lado del paciente): la conversacion que espera al medico reacciona a su
 * respuesta. Solo reacciona una vez y solo a la solicitud que esta esperando; las opciones que el
 * medico propone se vuelven la nueva lista blanca, mas "Ninguno me funciona".
 */
class ConversationRequestOutcomeTest {

    private static final String PHONE = "5215512345678";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 24, 10, 0);
    private static final AvailableSlot OPTION_1 = new AvailableSlot(NOW.plusDays(4).withHour(10), NOW.plusDays(4).withHour(11));
    private static final AvailableSlot OPTION_2 = new AvailableSlot(NOW.plusDays(5).withHour(12), NOW.plusDays(5).withHour(13));

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();

    private final FakePatients patients = new FakePatients();
    private final FakeDoctors doctors = new FakeDoctors();
    private final FakeSlots slots = new FakeSlots();
    private final FakeRequests requests = new FakeRequests();
    private final InMemoryConversations conversations = new InMemoryConversations();
    private final MutableClock clock = new MutableClock(NOW);

    private ConversationService service;

    @BeforeEach
    void setUp() {
        service = new ConversationService(conversations, patients, doctors, slots, requests, new FakeInterpreter(),
                new ConversationNewPatientTest.FakeClinicInfo(), new NewPatientRegistration(conversations,
                new ConversationNewPatientTest.FakeRegistrations(), new ConversationNewPatientTest.InMemoryDrafts(), clock),
                clock);
        patients.add(PHONE, patientId, "Ana López");
        doctors.add(doctorId, "Dra. Beatriz Ramos");
        doctors.lastDoctor.put(patientId, doctorId);
        slots.add(doctorId, NOW.plusDays(1).withHour(10), NOW.plusDays(1).withHour(11));
    }

    // ---- respuesta del medico ---------------------------------------------------------------

    @Test
    void anAcceptedRequestConfirmsTheAppointmentAndClosesTheConversation() {
        Conversation waiting = waitForDoctor();

        PatientNotification notification = service.onRequestResolved(event(waiting, Outcome.BOOKED, null)).orElseThrow();

        assertEquals(clinicId, notification.clinicId());
        assertEquals(PHONE, notification.phone());
        assertTrue(notification.reply().text().contains("confirmada"), notification.reply().text());
        assertTrue(notification.reply().text().contains("Beatriz"), notification.reply().text());
        assertTrue(notification.reply().options().isEmpty());
        assertEquals(ConversationState.CERRADA, conversations.lastSaved.state());
    }

    @Test
    void aRejectedRequestExplainsWhyAndOffersTheDoctorsSlotsAgain() {
        Conversation waiting = waitForDoctor();

        OutboundReply reply = service.onRequestResolved(event(waiting, Outcome.REJECTED, "Estaré en un congreso"))
                .orElseThrow().reply();

        assertTrue(reply.text().contains("no puede atenderte"), reply.text());
        assertTrue(reply.text().contains("Estaré en un congreso"), reply.text());
        assertEquals(ConversationState.ELEGIR_CUPO, current().state());
        assertTrue(reply.options().stream().allMatch(option -> option.id().startsWith("slot:")));
        assertEquals(2, reply.options().size());
    }

    @Test
    void theProposedOptionsBecomeTheOnlyChoices() {
        Conversation waiting = waitForDoctor();

        OutboundReply reply = propose(waiting, OPTION_1, OPTION_2);

        assertTrue(reply.text().contains("propone"), reply.text());
        assertEquals(ConversationState.ELEGIR_OPCION_MEDICO, current().state());
        assertEquals(waiting.requestId(), current().requestId());
        assertEquals(List.of(ConversationService.slotId(OPTION_1), ConversationService.slotId(OPTION_2),
                ConversationService.DECLINE_OPTIONS), optionIds(reply));
    }

    @Test
    void anExpiredRequestReturnsThePatientToTheMenu() {
        Conversation waiting = waitForDoctor();

        OutboundReply reply = service.onRequestResolved(event(waiting, Outcome.EXPIRED, null)).orElseThrow().reply();

        assertTrue(reply.text().contains("venció"), reply.text());
        assertEquals(ConversationState.MENU, current().state());
        assertEquals(patientId, current().patientId());
    }

    @Test
    void aRepeatedOrStaleEventIsIgnored() {
        Conversation waiting = waitForDoctor();
        service.onRequestResolved(event(waiting, Outcome.BOOKED, null));

        assertTrue(service.onRequestResolved(event(waiting, Outcome.BOOKED, null)).isEmpty(), "evento repetido");
        Conversation other = waitForDoctor();
        AppointmentRequestResolvedEvent stale = new AppointmentRequestResolvedEvent(UUID.randomUUID(), clinicId,
                UUID.randomUUID(), other.id(), Outcome.REJECTED, "Dra. Beatriz Ramos", null, null, null, List.of(), NOW);
        assertTrue(service.onRequestResolved(stale).isEmpty(), "de otra solicitud");
        assertEquals(ConversationState.ESPERANDO_MEDICO, current().state());
        AppointmentRequestResolvedEvent orphan = new AppointmentRequestResolvedEvent(UUID.randomUUID(), clinicId,
                UUID.randomUUID(), UUID.randomUUID(), Outcome.BOOKED, "Dra. Beatriz Ramos", null, null, null, List.of(), NOW);
        assertTrue(service.onRequestResolved(orphan).isEmpty(), "conversacion inexistente");
    }

    // ---- eleccion entre las opciones propuestas -------------------------------------------------

    @Test
    void choosingAProposedOptionBooksItDirectly() {
        Conversation waiting = waitForDoctor();
        propose(waiting, OPTION_1, OPTION_2);

        OutboundReply reply = send(null, ConversationService.slotId(OPTION_2));

        assertEquals(List.of(OPTION_2), requests.chosen);
        assertEquals(List.of(waiting.requestId()), requests.chosenRequests);
        assertTrue(reply.text().contains("confirmada"), reply.text());
        assertEquals(ConversationState.CERRADA, conversations.lastSaved.state());
    }

    @Test
    void aProposedOptionThatWasTakenIsDroppedAndTheRestOfferedAgain() {
        propose(waitForDoctor(), OPTION_1, OPTION_2);
        requests.failChoice = true;

        OutboundReply reply = send(null, ConversationService.slotId(OPTION_1));

        assertTrue(reply.text().contains("ya no está disponible"), reply.text());
        assertEquals(List.of(ConversationService.slotId(OPTION_2), ConversationService.DECLINE_OPTIONS), optionIds(reply));
        assertEquals(ConversationState.ELEGIR_OPCION_MEDICO, current().state());
    }

    @Test
    void whenNoProposedOptionRemainsTheProposalIsClosed() {
        Conversation waiting = waitForDoctor();
        propose(waiting, OPTION_1);
        requests.failChoice = true;

        send(null, ConversationService.slotId(OPTION_1));

        assertEquals(List.of(waiting.requestId()), requests.declined);
        assertEquals(ConversationState.MENU, current().state());
    }

    @Test
    void decliningTheOptionsReleasesThemAndReturnsToTheMenu() {
        Conversation waiting = waitForDoctor();
        propose(waiting, OPTION_1, OPTION_2);

        send(null, ConversationService.DECLINE_OPTIONS);

        assertEquals(List.of(waiting.requestId()), requests.declined);
        assertEquals(ConversationState.MENU, current().state());
    }

    @Test
    void restartingWhileChoosingAlsoDeclinesTheProposal() {
        Conversation waiting = waitForDoctor();
        propose(waiting, OPTION_1);

        send("menú", null);

        assertEquals(List.of(waiting.requestId()), requests.declined);
        assertEquals(ConversationState.MENU, current().state());
    }

    @Test
    void anOptionThatWasNotProposedIsNeverBooked() {
        propose(waitForDoctor(), OPTION_1);

        send(null, ConversationService.slotId(new AvailableSlot(NOW.plusDays(9).withHour(9), NOW.plusDays(9).withHour(10))));

        assertTrue(requests.chosen.isEmpty());
        assertEquals(ConversationState.ELEGIR_OPCION_MEDICO, current().state());
    }

    @Test
    void thePatientHasMoreThanThirtyMinutesToChooseAnOption() {
        propose(waitForDoctor(), OPTION_1);
        clock.advance(Duration.ofHours(3));

        send(null, ConversationService.slotId(OPTION_1));

        assertEquals(List.of(OPTION_1), requests.chosen, "la propuesta vence con la solicitud (24 h), no por inactividad");
    }

    @Test
    void anExpiredProposalAlsoReturnsToTheMenu() {
        Conversation waiting = waitForDoctor();
        propose(waiting, OPTION_1);

        OutboundReply reply = service.onRequestResolved(event(waiting, Outcome.EXPIRED, null)).orElseThrow().reply();

        assertTrue(reply.text().contains("venc"), reply.text());
        assertEquals(ConversationState.MENU, current().state());
    }

    // ---- utilidades -------------------------------------------------------------------------

    private Conversation waitForDoctor() {
        send("Hola", null);
        send(null, ConversationService.BOOK);
        OutboundReply slotList = send(null, ConversationService.LAST_DOCTOR);
        send(null, slotList.options().get(0).id());
        Conversation waiting = current();
        assertEquals(ConversationState.ESPERANDO_MEDICO, waiting.state());
        return waiting;
    }

    private OutboundReply propose(Conversation waiting, AvailableSlot... options) {
        return service.onRequestResolved(new AppointmentRequestResolvedEvent(UUID.randomUUID(), clinicId,
                waiting.requestId(), waiting.id(), Outcome.OPTIONS_PROPOSED, "Dra. Beatriz Ramos",
                NOW.plusDays(1).withHour(10), NOW.plusDays(1).withHour(11), null, List.of(options), NOW))
                .orElseThrow().reply();
    }

    private AppointmentRequestResolvedEvent event(Conversation waiting, Outcome outcome, String reason) {
        return new AppointmentRequestResolvedEvent(UUID.randomUUID(), clinicId, waiting.requestId(), waiting.id(),
                outcome, "Dra. Beatriz Ramos", NOW.plusDays(1).withHour(10), NOW.plusDays(1).withHour(11), reason,
                List.of(), NOW);
    }

    private OutboundReply send(String text, String optionId) {
        List<OutboundReply> replies = service.handle(new InboundMessage(clinicId, PHONE, text, optionId, clock.now()));
        assertEquals(1, replies.size());
        return replies.get(0);
    }

    private Conversation current() {
        return conversations.findActive(clinicId, PHONE).orElseThrow();
    }

    private static List<String> optionIds(OutboundReply reply) {
        return reply.options().stream().map(ConversationOption::id).toList();
    }
}
