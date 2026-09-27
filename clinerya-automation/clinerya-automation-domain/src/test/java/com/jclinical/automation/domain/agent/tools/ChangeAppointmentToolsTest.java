package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.tools.BookingToolsTest.FakeRequests;
import com.jclinical.automation.domain.agent.tools.BookingToolsTest.InMemoryConversations;
import com.jclinical.automation.domain.agent.tools.BookingToolsTest.InMemoryPending;
import com.jclinical.automation.domain.agent.tools.ReadToolsTest.FakeDoctors;
import com.jclinical.automation.domain.model.AppointmentRequest;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.AppointmentCancellationPort;
import com.jclinical.automation.domain.ports.out.DoctorAlertPort;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort.UpcomingVisit;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cancelar y reprogramar por el agente, siempre en dos pasos y solo sobre citas del propio paciente
 * (lo verifica el codigo con la agenda). Reprogramar es con el mismo medico y la cita original se
 * conserva hasta que el medico aprueba el cambio.
 */
class ChangeAppointmentToolsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 27, 21, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID ramos = UUID.randomUUID();
    private final UUID diaz = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();
    private final PatientContact ana = new PatientContact(UUID.randomUUID(), "Ana López");
    private final LocalDateTime monday = LocalDateTime.of(2026, 9, 28, 10, 0);
    private final FakeDoctors doctors = new FakeDoctors();
    private final InMemoryPending pending = new InMemoryPending();
    private final FakeRequests requests = new FakeRequests();
    private final InMemoryConversations conversations = new InMemoryConversations();
    private final List<String> cancelled = new ArrayList<>();
    private final List<String> doctorNotices = new ArrayList<>();
    private boolean agendaRefuses;
    private final Conversation conversation = new Conversation(UUID.randomUUID(), clinicId, "5215512345678",
            ConversationState.CONVERSANDO, null, null, null, null, null, List.of(), 0, NOW.minusMinutes(5), NOW.minusMinutes(5));

    private final PatientAppointmentsPort appointments = (clinic, patient, limit) -> patient.equals(ana.patientId())
            ? List.of(new UpcomingVisit(appointmentId, ramos, monday, monday.plusMinutes(30), false)) : List.of();
    private final AppointmentCancellationPort cancellation = (clinic, appointment, patient, reason) -> {
        if (agendaRefuses) throw new AppointmentCancellationPort.NotCancellableException("ya paso");
        cancelled.add(appointment + "|" + reason);
    };
    private final DoctorAlertPort alerts = new DoctorAlertPort() {
        @Override public void newRequest(AppointmentRequest request) { }
        @Override public void appointmentCancelled(UUID clinic, UUID doctor, LocalDateTime start) { doctorNotices.add(doctor + "|" + start); }
    };

    @BeforeEach
    void setUp() {
        doctors.add(ramos, "Dra. Beatriz Ramos");
        doctors.add(diaz, "Dr. Carlos Díaz");
        conversations.save(conversation);
    }

    private ToolContext context(LocalDateTime now, List<ConversationOption> offered) {
        return new ToolContext(clinicId, conversation.id(), conversation.phone(), List.of(ana), offered, now);
    }

    private ConfirmActionTool confirm() {
        return new ConfirmActionTool(pending, requests, conversations, cancellation, alerts);
    }

    private ConversationOption slotOf(UUID doctor, LocalDateTime start) {
        return new ConversationOption("slot:" + doctor + "|" + start + "|" + start.plusMinutes(30), "Jue 01/10 16:00");
    }

    // ---- cancelar -------------------------------------------------------------------------------

    @Test
    void proposingToCancelTheirOwnAppointmentKeepsItPending() {
        ToolOutcome outcome = new ProposeCancellationTool(appointments, doctors, pending)
                .run(context(NOW, List.of()), Map.of("cita", "cita:" + appointmentId, "motivo", "Tengo un viaje"));

        PendingAction action = pending.find(conversation.id()).orElseThrow();
        assertEquals(PendingAction.Kind.CANCEL, action.kind());
        assertEquals(appointmentId, action.appointmentId());
        assertEquals("Tengo un viaje", action.note());
        assertEquals(ConfirmActionTool.CONFIRMATION_OPTIONS, outcome.options());
    }

    @Test
    void anAppointmentThatIsNotTheirsCannotBeProposed() {
        ToolOutcome outcome = new ProposeCancellationTool(appointments, doctors, pending)
                .run(context(NOW, List.of()), Map.of("cita", "cita:" + UUID.randomUUID()));

        assertTrue(outcome.content().containsKey("error"));
        assertTrue(pending.find(conversation.id()).isEmpty());
    }

    @Test
    void confirmingTheCancellationCancelsItAndTellsTheDoctor() {
        new ProposeCancellationTool(appointments, doctors, pending)
                .run(context(NOW, List.of()), Map.of("cita", "cita:" + appointmentId, "motivo", "Tengo un viaje"));

        ToolOutcome outcome = confirm().run(context(NOW.plusMinutes(1), List.of()), Map.of());

        assertEquals(List.of(appointmentId + "|Tengo un viaje"), cancelled);
        assertEquals(List.of(ramos + "|" + monday), doctorNotices);
        assertEquals(true, outcome.content().get("cita_cancelada"));
    }

    @Test
    void aCancellationTheAgendaRefusesIsReported() {
        new ProposeCancellationTool(appointments, doctors, pending).run(context(NOW, List.of()), Map.of("cita", "cita:" + appointmentId));
        agendaRefuses = true;

        ToolOutcome outcome = confirm().run(context(NOW.plusMinutes(1), List.of()), Map.of());

        assertTrue(outcome.content().containsKey("error"));
        assertTrue(doctorNotices.isEmpty());
    }

    // ---- reprogramar ----------------------------------------------------------------------------

    @Test
    void reschedulingNeedsAnOfferedSlotOfTheSameDoctor() {
        LocalDateTime thursday = LocalDateTime.of(2026, 10, 1, 16, 0);
        ConversationOption otherDoctor = slotOf(diaz, thursday);
        ConversationOption sameDoctor = slotOf(ramos, thursday);
        ProposeRescheduleTool tool = new ProposeRescheduleTool(appointments, doctors, pending);

        ToolOutcome refused = tool.run(context(NOW, List.of(otherDoctor)), Map.of("cita", "cita:" + appointmentId, "horario", otherDoctor.id()));
        assertTrue(refused.content().containsKey("error"));
        assertTrue(pending.find(conversation.id()).isEmpty());

        tool.run(context(NOW, List.of(sameDoctor)), Map.of("cita", "cita:" + appointmentId, "horario", sameDoctor.id()));
        PendingAction action = pending.find(conversation.id()).orElseThrow();
        assertEquals(PendingAction.Kind.RESCHEDULE, action.kind());
        assertEquals(appointmentId, action.appointmentId());
        assertEquals(thursday, action.start());
    }

    @Test
    void confirmingARescheduleSendsARequestThatReplacesTheOriginalWithoutCancellingItYet() {
        ConversationOption slot = slotOf(ramos, LocalDateTime.of(2026, 10, 1, 16, 0));
        new ProposeRescheduleTool(appointments, doctors, pending)
                .run(context(NOW, List.of(slot)), Map.of("cita", "cita:" + appointmentId, "horario", slot.id()));

        confirm().run(context(NOW.plusMinutes(1), List.of()), Map.of());

        assertEquals(appointmentId, requests.submitted.getFirst().replacesAppointmentId());
        assertEquals(requests.nextId, conversations.findById(conversation.id()).orElseThrow().requestId());
        assertTrue(cancelled.isEmpty(), "la original se cancela hasta que el medico aprueba");
    }

    // ---- mis citas para elegir cual cambiar ---------------------------------------------------

    @Test
    void whenChoosingWhichOneToChangeTheAppointmentsComeAsAList() {
        UUID second = UUID.randomUUID();
        PatientAppointmentsPort two = (clinic, patient, limit) -> List.of(
                new UpcomingVisit(appointmentId, ramos, monday, monday.plusMinutes(30), false),
                new UpcomingVisit(second, diaz, monday.plusDays(1), monday.plusDays(1).plusMinutes(30), true));

        ToolOutcome plain = new MyAppointmentsTool(two, doctors).run(context(NOW, List.of()), Map.of());
        ToolOutcome choosing = new MyAppointmentsTool(two, doctors).run(context(NOW, List.of()), Map.of("para", "cancelar"));

        assertTrue(plain.options().isEmpty());
        assertEquals(List.of("cita:" + appointmentId, "cita:" + second), choosing.options().stream().map(ConversationOption::id).toList());
        assertEquals("Lun 28/09 10:00 · Dra. Beatriz Ramos", choosing.options().getFirst().label());
    }
}
