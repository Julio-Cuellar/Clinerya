package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.tools.ReadToolsTest.FakeDoctors;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;
import com.jclinical.automation.domain.ports.out.PendingActionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agendar en dos pasos. Proponer solo acepta un horario que se le mostro al paciente y guarda la
 * accion pendiente; confirmar solo la ejecuta si el paciente respondio en un mensaje posterior a la
 * propuesta. La cita sigue necesitando la aprobacion del medico.
 */
class BookingToolsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 27, 21, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID ramos = UUID.randomUUID();
    private final PatientContact ana = new PatientContact(UUID.randomUUID(), "Ana López");
    private final PatientContact luis = new PatientContact(UUID.randomUUID(), "Luis López");
    private final FakeDoctors doctors = new FakeDoctors();
    private final InMemoryPending pending = new InMemoryPending();
    private final FakeRequests requests = new FakeRequests();
    private final InMemoryConversations conversations = new InMemoryConversations();
    private final Conversation conversation = new Conversation(UUID.randomUUID(), clinicId, "5215512345678",
            ConversationState.CONVERSANDO, null, null, null, null, null, List.of(), 0, NOW.minusMinutes(5), NOW.minusMinutes(5));

    private final LocalDateTime thursday = LocalDateTime.of(2026, 10, 1, 16, 0);
    private final ConversationOption slot = new ConversationOption(
            "slot:" + ramos + "|" + thursday + "|" + thursday.plusMinutes(30), "Jue 01/10 16:00");

    @BeforeEach
    void setUp() {
        doctors.add(ramos, "Dra. Beatriz Ramos");
        conversations.save(conversation);
    }

    private ToolContext context(LocalDateTime now, List<ConversationOption> offered, PatientContact... patients) {
        return new ToolContext(clinicId, conversation.id(), conversation.phone(), List.of(patients), offered, now);
    }

    private ProposeBookingTool propose() {
        return new ProposeBookingTool(doctors, pending);
    }

    private ConfirmActionTool confirm() {
        return new ConfirmActionTool(pending, requests, conversations, (clinic, appointment, patient, reason) -> { },
                request -> { });
    }

    // ---- proponer -------------------------------------------------------------------------------

    @Test
    void proposingAnOfferedSlotKeepsItPendingAndAsksToConfirm() {
        ToolOutcome outcome = propose().run(context(NOW, List.of(slot), ana), Map.of("horario", slot.id()));

        PendingAction saved = pending.find(conversation.id()).orElseThrow();
        assertEquals(new PendingAction(conversation.id(), PendingAction.Kind.BOOK, ana.patientId(), "Ana López", ramos,
                "Dra. Beatriz Ramos", thursday, thursday.plusMinutes(30), null, NOW), saved);
        assertEquals(List.of(ConfirmActionTool.CONFIRM, ConfirmActionTool.CHANGE),
                outcome.options().stream().map(ConversationOption::id).toList());
        String summary = String.valueOf(outcome.content().get("resumen"));
        assertTrue(summary.contains("Jue 01/10 16:00") && summary.contains("Dra. Beatriz Ramos") && summary.contains("Ana"), summary);
    }

    @Test
    void aSlotThatWasNotShownToThePatientIsRejected() {
        ToolOutcome outcome = propose().run(context(NOW, List.of(), ana), Map.of("horario", slot.id()));

        assertTrue(outcome.content().containsKey("error"));
        assertTrue(pending.find(conversation.id()).isEmpty());
    }

    @Test
    void anUnregisteredNumberMustRegisterFirst() {
        ToolOutcome outcome = propose().run(context(NOW, List.of(slot)), Map.of("horario", slot.id()));

        assertEquals(true, outcome.content().get("registro_necesario"));
        assertTrue(pending.find(conversation.id()).isEmpty());
    }

    @Test
    void withSeveralPatientsOnThePhoneItAsksForWhom() {
        ToolOutcome ask = propose().run(context(NOW, List.of(slot), ana, luis), Map.of("horario", slot.id()));
        assertTrue(ask.content().containsKey("error"));
        assertEquals(List.of("paciente:" + ana.patientId(), "paciente:" + luis.patientId()),
                ask.options().stream().map(ConversationOption::id).toList());

        propose().run(context(NOW, List.of(slot), ana, luis), Map.of("horario", slot.id(), "paciente", "paciente:" + luis.patientId()));
        assertEquals(luis.patientId(), pending.find(conversation.id()).orElseThrow().patientId());
    }

    // ---- confirmar ------------------------------------------------------------------------------

    @Test
    void confirmingInTheSameMessageAsTheProposalIsRefused() {
        propose().run(context(NOW, List.of(slot), ana), Map.of("horario", slot.id()));

        ToolOutcome outcome = confirm().run(context(NOW, List.of(), ana), Map.of());

        assertTrue(outcome.content().containsKey("error"));
        assertTrue(requests.submitted.isEmpty());
    }

    @Test
    void confirmingInALaterMessageSendsTheRequestToTheDoctorAndRemembersIt() {
        propose().run(context(NOW, List.of(slot), ana), Map.of("horario", slot.id()));

        ToolOutcome outcome = confirm().run(context(NOW.plusMinutes(1), List.of(), ana), Map.of());

        assertEquals(1, requests.submitted.size());
        AppointmentRequestPort.NewAppointmentRequest sent = requests.submitted.getFirst();
        assertEquals(new AppointmentRequestPort.NewAppointmentRequest(clinicId, conversation.id(), ana.patientId(), ramos,
                thursday, thursday.plusMinutes(30), conversation.phone(), "Ana López", "Dra. Beatriz Ramos"), sent);
        assertEquals(true, outcome.content().get("solicitud_enviada"));
        assertTrue(outcome.closing() != null && outcome.closing().contains("solicitud")
                && outcome.closing().contains("Dra. Beatriz Ramos") && outcome.closing().contains("Jue 01/10 16:00"),
                String.valueOf(outcome.closing()));
        assertEquals(requests.nextId, conversations.findById(conversation.id()).orElseThrow().requestId());
        assertTrue(pending.find(conversation.id()).isEmpty());
    }

    @Test
    void aSlotTakenMeanwhileIsReported() {
        propose().run(context(NOW, List.of(slot), ana), Map.of("horario", slot.id()));
        requests.taken = true;

        ToolOutcome outcome = confirm().run(context(NOW.plusMinutes(1), List.of(), ana), Map.of());

        assertTrue(outcome.content().containsKey("error"));
        assertTrue(outcome.closing() != null && outcome.closing().contains("ocupó"), String.valueOf(outcome.closing()));
        assertTrue(pending.find(conversation.id()).isEmpty());
    }

    @Test
    void thereIsNothingToConfirmWithoutAProposalOrAfterItExpires() {
        assertTrue(confirm().run(context(NOW, List.of(), ana), Map.of()).content().containsKey("error"));

        propose().run(context(NOW, List.of(slot), ana), Map.of("horario", slot.id()));
        ToolOutcome late = confirm().run(context(NOW.plusMinutes(31), List.of(), ana), Map.of());

        assertTrue(late.content().containsKey("error"));
        assertTrue(late.closing() != null && late.closing().contains("venció"), String.valueOf(late.closing()));
        assertTrue(requests.submitted.isEmpty());
        assertTrue(pending.find(conversation.id()).isEmpty());
    }

    // ---- propuesta del medico -------------------------------------------------------------------

    @Test
    void choosingAnOptionTheDoctorProposedBooksItDirectly() {
        UUID requestId = UUID.randomUUID();
        conversations.save(withRequest(requestId));
        LocalDateTime friday = LocalDateTime.of(2026, 10, 2, 10, 0);
        ConversationOption proposed = new ConversationOption(
                "propuesta:" + requestId + "|" + friday + "|" + friday.plusMinutes(30), "Vie 02/10 10:00");

        ToolOutcome outcome = new ChooseProposalTool(requests, conversations)
                .run(context(NOW, List.of(proposed, new ConversationOption(ChooseProposalTool.NONE, "Ninguno me funciona")), ana),
                        Map.of("opcion", proposed.id()));

        assertEquals(List.of(requestId + "|" + friday), requests.chosen);
        assertEquals(true, outcome.content().get("cita_agendada"));
        assertTrue(outcome.closing() != null && outcome.closing().contains("quedó agendada")
                && outcome.closing().contains("Vie 02/10 10:00"), String.valueOf(outcome.closing()));
        assertNull(conversations.findById(conversation.id()).orElseThrow().requestId());
    }

    @Test
    void noneOfTheProposalsReleasesThem() {
        UUID requestId = UUID.randomUUID();
        conversations.save(withRequest(requestId));

        new ChooseProposalTool(requests, conversations).run(
                context(NOW, List.of(new ConversationOption(ChooseProposalTool.NONE, "Ninguno me funciona")), ana),
                Map.of("opcion", ChooseProposalTool.NONE));

        assertEquals(List.of(requestId), requests.declined);
        assertNull(conversations.findById(conversation.id()).orElseThrow().requestId());
    }

    // ---- utilidades -----------------------------------------------------------------------------

    private Conversation withRequest(UUID requestId) {
        return new Conversation(conversation.id(), clinicId, conversation.phone(), ConversationState.CONVERSANDO, null, null,
                null, null, requestId, List.of(), 0, conversation.createdAt(), NOW.minusMinutes(1));
    }

    static final class InMemoryPending implements PendingActionPort {
        final Map<UUID, PendingAction> byConversation = new HashMap<>();

        @Override public void save(PendingAction action) { byConversation.put(action.conversationId(), action); }

        @Override public Optional<PendingAction> find(UUID conversationId) { return Optional.ofNullable(byConversation.get(conversationId)); }

        @Override public void clear(UUID conversationId) { byConversation.remove(conversationId); }
    }

    static final class FakeRequests implements AppointmentRequestPort {
        final List<NewAppointmentRequest> submitted = new ArrayList<>();
        final List<String> chosen = new ArrayList<>();
        final List<UUID> declined = new ArrayList<>();
        final UUID nextId = UUID.randomUUID();
        boolean taken;

        @Override
        public UUID submit(NewAppointmentRequest request) {
            if (taken) throw new SlotNoLongerAvailableException();
            submitted.add(request);
            return nextId;
        }

        @Override
        public UUID chooseOption(UUID clinicId, UUID requestId, LocalDateTime start, LocalDateTime end) {
            chosen.add(requestId + "|" + start);
            return UUID.randomUUID();
        }

        @Override public void declineOptions(UUID clinicId, UUID requestId) { declined.add(requestId); }
    }

    static final class InMemoryConversations implements ConversationRepositoryPort {
        final Map<UUID, Conversation> byId = new HashMap<>();

        @Override
        public Optional<Conversation> findActive(UUID clinicId, String phone) {
            return byId.values().stream().filter(c -> c.phone().equals(phone) && !c.state().isTerminal()).findFirst();
        }

        @Override public Optional<Conversation> findById(UUID conversationId) { return Optional.ofNullable(byId.get(conversationId)); }

        @Override public Conversation save(Conversation conversation) { byId.put(conversation.id(), conversation); return conversation; }
    }
}
