package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.tools.BookingToolsTest.InMemoryPending;
import com.jclinical.automation.domain.agent.tools.ReadToolsTest.FakeDoctors;
import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Alta de un paciente nuevo por el agente. La autorizacion de contacto se manda tal cual (la escribe
 * el codigo, no el modelo) y solo cuenta si la acepto el propio paciente en su mensaje; los datos se
 * validan igual que en el alta actual.
 */
class RegistrationToolsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 27, 21, 0);
    private static final String PHONE = "5215512345678";
    private static final String CONSENT = "¿Autoriza a la clínica a contactarle para recordatorios y seguimiento de su consulta?";

    private final UUID clinicId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID ramos = UUID.randomUUID();
    private final InMemoryPending pending = new InMemoryPending();
    private final FakeRegistrations registrations = new FakeRegistrations();
    private final FakeDoctors doctors = new FakeDoctors();
    private final LocalDateTime thursday = LocalDateTime.of(2026, 10, 1, 16, 0);
    private final ConversationOption slot = new ConversationOption(
            "slot:" + ramos + "|" + thursday + "|" + thursday.plusMinutes(30), "Jue 01/10 16:00");

    @BeforeEach
    void setUp() {
        doctors.add(ramos, "Dra. Beatriz Ramos");
    }

    private ToolContext context(String patientMessage, List<ConversationOption> offered, PatientContact... patients) {
        return new ToolContext(clinicId, conversationId, PHONE, List.of(patients), offered, NOW, patientMessage);
    }

    private ConsentTool consent() {
        return new ConsentTool(registrations, clinic -> Optional.of(new ClinicInfo("Clínica Sonrisa", null, null, null,
                "https://sonrisa.mx/privacidad", List.of())), pending);
    }

    private RegisterPatientTool register() {
        return new RegisterPatientTool(registrations, pending, doctors);
    }

    private void accepted() {
        pending.save(new PendingAction(conversationId, PendingAction.Kind.CONSENT_ACCEPTED, null, null, null, null, null, null,
                null, NOW.minusMinutes(5)));
    }

    // ---- consentimiento -------------------------------------------------------------------------

    @Test
    void theConsentGoesOutWordForWordWithButtonsAndStaysPending() {
        ToolOutcome outcome = consent().run(context("quiero una cita", List.of()), Map.of());

        assertTrue(outcome.verbatim().startsWith(CONSENT), outcome.verbatim());
        assertTrue(outcome.verbatim().contains("https://sonrisa.mx/privacidad"), outcome.verbatim());
        assertEquals(List.of(ConsentTool.ACCEPT, ConsentTool.DECLINE), outcome.options().stream().map(ConversationOption::id).toList());
        assertEquals(PendingAction.Kind.CONSENT, pending.find(conversationId).orElseThrow().kind());
    }

    @Test
    void aRegisteredNumberDoesNotNeedToRegister() {
        ToolOutcome outcome = consent().run(context("hola", List.of(), new PatientContact(UUID.randomUUID(), "Ana López")), Map.of());

        assertTrue(outcome.content().containsKey("error"));
        assertEquals(null, outcome.verbatim());
    }

    @Test
    void theConsentCountsOnlyIfThePatientAcceptedItThemselves() {
        consent().run(context("quiero una cita", List.of()), Map.of());
        AcceptConsentTool accept = new AcceptConsentTool(pending);

        ToolOutcome notYet = accept.run(context("quiero una cita el jueves", List.of()), Map.of());
        assertTrue(notYet.content().containsKey("error"));
        assertEquals(PendingAction.Kind.CONSENT, pending.find(conversationId).orElseThrow().kind());

        accept.run(context("Sí, acepto", List.of()), Map.of());
        assertEquals(PendingAction.Kind.CONSENT_ACCEPTED, pending.find(conversationId).orElseThrow().kind());
    }

    @Test
    void tappingAcceptCountsAndTappingDeclineClearsIt() {
        AcceptConsentTool accept = new AcceptConsentTool(pending);

        consent().run(context("hola", List.of()), Map.of());
        accept.run(context(ConsentTool.ACCEPT, List.of()), Map.of());
        assertEquals(PendingAction.Kind.CONSENT_ACCEPTED, pending.find(conversationId).orElseThrow().kind());

        consent().run(context("hola", List.of()), Map.of());
        ToolOutcome declined = accept.run(context(ConsentTool.DECLINE, List.of()), Map.of());
        assertEquals(true, declined.content().get("rechazado"));
        assertTrue(pending.find(conversationId).isEmpty());
    }

    // ---- registro -------------------------------------------------------------------------------

    @Test
    void registeringWithoutAcceptedConsentIsRefused() {
        ToolOutcome outcome = register().run(context("me llamo Juan", List.of()), data());

        assertTrue(outcome.content().containsKey("error"));
        assertTrue(registrations.registered.isEmpty());
    }

    @Test
    void aValidRegistrationCreatesThePatientWithTheConsentVersion() {
        accepted();

        ToolOutcome outcome = register().run(context("mi correo es juan@correo.com", List.of()), data());

        assertEquals(List.of(new PatientRegistrationPort.NewPatient(clinicId, "Juan", "Pérez", "López", LocalDate.of(1990, 3, 14),
                PatientRegistrationPort.Sex.MALE, PHONE, "juan@correo.com", "v1")), registrations.registered);
        assertEquals(true, outcome.content().get("paciente_registrado"));
        assertTrue(pending.find(conversationId).isEmpty());
        assertTrue(outcome.fallback() != null && outcome.fallback().contains("Juan"), String.valueOf(outcome.fallback()));
    }

    @Test
    void anInvalidBirthDateIsExplainedAndNothingIsSaved() {
        accepted();
        Map<String, Object> data = new java.util.HashMap<>(data());
        data.put("fecha_nacimiento", "31/02/1990");

        ToolOutcome outcome = register().run(context("31/02/1990", List.of()), data);

        assertTrue(String.valueOf(outcome.content().get("error")).contains("fecha"), outcome.content().toString());
        assertTrue(registrations.registered.isEmpty());
    }

    @Test
    void withoutSexItIsAskedWithButtons() {
        accepted();
        Map<String, Object> data = new java.util.HashMap<>(data());
        data.remove("sexo");

        ToolOutcome outcome = register().run(context("Juan Pérez López", List.of()), data);

        assertEquals(List.of(RegisterPatientTool.SEX_FEMALE, RegisterPatientTool.SEX_MALE, RegisterPatientTool.SEX_OTHER),
                outcome.options().stream().map(ConversationOption::id).toList());
        assertTrue(registrations.registered.isEmpty());
    }

    @Test
    void aSlotAlreadyChosenIsLeftReadyToConfirm() {
        accepted();
        Map<String, Object> data = new java.util.HashMap<>(data());
        data.put("horario", slot.id());

        ToolOutcome outcome = register().run(context("listo", List.of(slot)), data);

        PendingAction booking = pending.find(conversationId).orElseThrow();
        assertEquals(PendingAction.Kind.BOOK, booking.kind());
        assertEquals(registrations.newId, booking.patientId());
        assertEquals(thursday, booking.start());
        assertEquals(ConfirmActionTool.CONFIRMATION_OPTIONS, outcome.options());
        assertTrue(outcome.fallback() != null && outcome.fallback().contains("¿Confirmo"), String.valueOf(outcome.fallback()));
    }

    // ---- utilidades -----------------------------------------------------------------------------

    private static Map<String, Object> data() {
        return Map.of("nombre", "Juan", "apellidos", "Pérez López", "fecha_nacimiento", "14/03/1990", "sexo", "masculino",
                "correo", "juan@correo.com");
    }

    static final class FakeRegistrations implements PatientRegistrationPort {
        final List<NewPatient> registered = new ArrayList<>();
        final UUID newId = UUID.randomUUID();

        @Override public ConsentText consentText() { return new ConsentText("v1", CONSENT); }

        @Override public UUID register(NewPatient patient) { registered.add(patient); return newId; }
    }
}
