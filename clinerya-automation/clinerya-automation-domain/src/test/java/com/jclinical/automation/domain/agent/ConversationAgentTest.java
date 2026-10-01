package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.agent.AgentMessage.ToolCall;
import com.jclinical.automation.domain.agent.AgentMessage.ToolResult;
import com.jclinical.automation.domain.agent.AgentMessage.User;
import com.jclinical.automation.domain.model.ConversationOption;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El modelo conduce la conversacion; el codigo pone los rieles: ejecuta las herramientas con el
 * contexto que el mismo arma (nunca con el que diga el modelo), limita los pasos y, si el modelo no
 * responde, contesta con honestidad y pasa el chat a una persona.
 */
class ConversationAgentTest {

    private final UUID clinicId = UUID.randomUUID();
    private final ToolContext context = new ToolContext(clinicId, UUID.randomUUID(), "5215512345678", List.of(), List.of(),
            LocalDateTime.of(2026, 9, 27, 18, 30));
    private final List<AgentMessage> transcript = List.of(new User("hola, ¿dónde están?"));

    @Test
    void theModelsWordsGoOutInUpToThreeBubbles() {
        ScriptedModel model = new ScriptedModel(new ModelStep.Reply(List.of("¡Hola!", "  ", "Estamos en el centro.", "¿Te ayudo?", "Saludos")));

        AgentOutcome outcome = new ConversationAgent(model, List.of()).run(context, "instrucciones", transcript);

        assertEquals(3, outcome.bubbles().size(), outcome.bubbles().toString());
        assertEquals("¡Hola!", outcome.bubbles().get(0));
        assertEquals("¿Te ayudo?\n\nSaludos", outcome.bubbles().get(2), "lo que sobra se une a la ultima burbuja");
        assertFalse(outcome.failed());
    }

    @Test
    void toolsRunWithTheContextTheCodeBuildsNotWithWhatTheModelSays() {
        RecordingTool tool = new RecordingTool("info_clinica", ToolOutcome.of(Map.of("direccion", "Av. Juárez 120")));
        UUID otherClinic = UUID.randomUUID();
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "info_clinica", Map.of("clinicId", otherClinic.toString())))),
                new ModelStep.Reply(List.of("Estamos en Av. Juárez 120.")));

        AgentOutcome outcome = new ConversationAgent(model, List.of(tool)).run(context, "instrucciones", transcript);

        assertEquals(clinicId, tool.lastContext.clinicId(), "la clinica la pone el codigo, no el modelo");
        assertEquals(List.of("Estamos en Av. Juárez 120."), outcome.bubbles());
        List<AgentMessage> second = model.transcripts.get(1);
        assertTrue(second.contains(new ToolResult("c1", "info_clinica", Map.of("direccion", "Av. Juárez 120"))), second.toString());
    }

    @Test
    void theModelSeesTheToolsAndTheBuiltInSignals() {
        ScriptedModel model = new ScriptedModel(new ModelStep.Reply(List.of("Hola")));

        new ConversationAgent(model, List.of(new RecordingTool("info_clinica", ToolOutcome.of(Map.of()))))
                .run(context, "instrucciones", transcript);

        List<String> names = model.tools.get(0).stream().map(ToolSpec::name).toList();
        assertTrue(names.containsAll(List.of("info_clinica", ConversationAgent.NOT_UNDERSTOOD, ConversationAgent.HANDOFF)), names.toString());
    }

    @Test
    void anUnknownToolIsReportedBackToTheModel() {
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "borrar_todo", Map.of()))),
                new ModelStep.Reply(List.of("Perdón, ¿me repites?")));

        new ConversationAgent(model, List.of()).run(context, "instrucciones", transcript);

        ToolResult result = (ToolResult) model.transcripts.get(1).getLast();
        assertTrue(result.content().containsKey("error"), result.toString());
    }

    @Test
    void aFailingToolIsReportedWithoutLeakingItsDetails() {
        AgentTool broken = new AgentTool() {
            @Override public ToolSpec spec() { return new ToolSpec("buscar_horarios", "Horarios", List.of()); }
            @Override public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
                throw new IllegalStateException("jdbc:postgresql://secreto");
            }
        };
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "buscar_horarios", Map.of()))),
                new ModelStep.Reply(List.of("Déjame revisarlo.")));

        new ConversationAgent(model, List.of(broken)).run(context, "instrucciones", transcript);

        String result = model.transcripts.get(1).getLast().toString();
        assertTrue(result.contains("error"), result);
        assertFalse(result.contains("postgresql"), "el detalle tecnico no viaja al modelo: " + result);
    }

    @Test
    void aModelFailureIsRetriedOnceInSilence() {
        ScriptedModel model = new ScriptedModel(new IllegalStateException("503"), new ModelStep.Reply(List.of("¡Hola!")));

        AgentOutcome outcome = new ConversationAgent(model, List.of()).run(context, "instrucciones", transcript);

        assertEquals(List.of("¡Hola!"), outcome.bubbles());
        assertFalse(outcome.failed());
    }

    @Test
    void ifTheModelKeepsFailingItAsksToRepeatWithoutHandingOff() {
        ScriptedModel model = new ScriptedModel(new IllegalStateException("503"), new IllegalStateException("503"));

        AgentOutcome outcome = new ConversationAgent(model, List.of()).run(context, "instrucciones", transcript);

        assertTrue(outcome.failed());
        assertFalse(outcome.handoff(), "una falla suelta no pasa el chat a una persona: la regla de 3 lo decide");
        assertEquals(List.of(ConversationAgent.UNAVAILABLE_REPLY), outcome.bubbles());
        assertFalse(ConversationAgent.UNAVAILABLE_REPLY.contains("alguien de la clínica"), ConversationAgent.UNAVAILABLE_REPLY);
    }

    @Test
    void anEmptyAnswerAfterAToolGetsANudgeToReplyWithTheResult() {
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "info_clinica", Map.of()))),
                new ModelStep.Reply(List.of()),
                new ModelStep.Reply(List.of("Estamos en el centro.")));

        AgentOutcome outcome = new ConversationAgent(model, List.of(new RecordingTool("info_clinica", ToolOutcome.of(Map.of()))))
                .run(context, "instrucciones", transcript);

        assertEquals(List.of("Estamos en el centro."), outcome.bubbles());
        assertFalse(outcome.failed());
        AgentMessage nudge = model.transcripts.get(2).getLast();
        assertTrue(nudge instanceof AgentMessage.Note, nudge.toString());
    }

    @Test
    void ifTheModelFailsAfterAnActionTheToolsOwnFallbackGoesOut() {
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "registrar_paciente", Map.of()))),
                new IllegalStateException("vacía"), new IllegalStateException("vacía"));
        ToolOutcome registered = ToolOutcome.of(Map.of("paciente_registrado", true))
                .withFallback("¡Listo, Juan! Ya quedaste registrado.");

        AgentOutcome outcome = new ConversationAgent(model, List.of(new RecordingTool("registrar_paciente", registered)))
                .run(context, "instrucciones", transcript);

        assertEquals(List.of("¡Listo, Juan! Ya quedaste registrado."), outcome.bubbles());
        assertFalse(outcome.failed(), "lo importante ya se hizo y se le dijo");
        assertFalse(outcome.handoff());
    }

    @Test
    void ifTheModelFailsAfterOfferingOptionsTheOptionsStillGoOut() {
        List<ConversationOption> slots = List.of(new ConversationOption("slot:a", "Jue 16:00"));
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "buscar_horarios", Map.of()))),
                new IllegalStateException("vacía"), new IllegalStateException("vacía"));

        AgentOutcome outcome = new ConversationAgent(model,
                List.of(new RecordingTool("buscar_horarios", ToolOutcome.of(Map.of()).withOptions(slots))))
                .run(context, "instrucciones", transcript);

        assertEquals(List.of(ConversationAgent.OPTIONS_REPLY), outcome.bubbles());
        assertEquals(slots, outcome.options());
        assertFalse(outcome.handoff());
    }

    @Test
    void aReplyThatIntroducesOptionsWithoutHavingAnyIsCorrected() {
        List<ConversationOption> slots = List.of(new ConversationOption("slot:a", "Jue 16:00"));
        ScriptedModel model = new ScriptedModel(
                new ModelStep.Reply(List.of("Estos son los horarios disponibles con el Dr. Julio:")),
                new ModelStep.CallTools(List.of(new ToolCall("c1", "buscar_horarios", Map.of()))),
                new ModelStep.Reply(List.of("Estos son los horarios disponibles con el Dr. Julio:")));

        AgentOutcome outcome = new ConversationAgent(model,
                List.of(new RecordingTool("buscar_horarios", ToolOutcome.of(Map.of()).withOptions(slots))))
                .run(context, "instrucciones", transcript);

        assertEquals(slots, outcome.options(), "la lista que anuncia tiene que llegar");
        AgentMessage note = model.transcripts.get(1).getLast();
        assertTrue(note instanceof AgentMessage.Note, note.toString());
    }

    @Test
    void sayingTheAppointmentIsBookedIsCorrectedBecauseOnlyTheDoctorBooksIt() {
        ScriptedModel model = new ScriptedModel(
                new ModelStep.Reply(List.of("¡Listo, Julio! Ya estás registrado y tu cita del jueves ha quedado agendada.")),
                new ModelStep.Reply(List.of("¡Listo, Julio! Ya estás registrado. ¿Confirmo tu cita del jueves?")));

        AgentOutcome outcome = new ConversationAgent(model, List.of()).run(context, "instrucciones", transcript);

        assertEquals(List.of("¡Listo, Julio! Ya estás registrado. ¿Confirmo tu cita del jueves?"), outcome.bubbles());
        AgentMessage note = model.transcripts.get(1).getLast();
        assertTrue(note instanceof AgentMessage.Note && ((AgentMessage.Note) note).text().contains("médico"), note.toString());
    }

    @Test
    void mentioningAnAppointmentTheyAlreadyHaveIsNotABookingClaim() {
        ScriptedModel model = new ScriptedModel(new ModelStep.Reply(List.of("Tienes una cita agendada el jueves con el Dr. Julio.")));

        AgentOutcome outcome = new ConversationAgent(model, List.of()).run(context, "instrucciones", transcript);

        assertEquals(1, model.transcripts.size());
        assertFalse(outcome.failed());
    }

    @Test
    void anEndlessToolLoopStopsWithoutHandingOff() {
        List<Object> steps = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            steps.add(new ModelStep.CallTools(List.of(new ToolCall("c" + i, "info_clinica", Map.of()))));
        }
        ScriptedModel model = new ScriptedModel(steps.toArray());

        AgentOutcome outcome = new ConversationAgent(model, List.of(new RecordingTool("info_clinica", ToolOutcome.of(Map.of()))))
                .run(context, "instrucciones", transcript);

        assertFalse(outcome.handoff());
        assertTrue(outcome.failed());
        assertEquals(ConversationAgent.MAX_STEPS, model.transcripts.size());
    }

    @Test
    void anEmptyReplyCountsAsAFailure() {
        ScriptedModel model = new ScriptedModel(new ModelStep.Reply(List.of(" ")), new ModelStep.Reply(List.of()));

        AgentOutcome outcome = new ConversationAgent(model, List.of()).run(context, "instrucciones", transcript);

        assertTrue(outcome.failed());
    }

    @Test
    void theBuiltInSignalsMarkNotUnderstoodAndHandoff() {
        ScriptedModel notUnderstood = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", ConversationAgent.NOT_UNDERSTOOD, Map.of()))),
                new ModelStep.Reply(List.of("Perdona, no te entendí bien. ¿Me lo cuentas de otra forma?")));
        ScriptedModel handoff = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", ConversationAgent.HANDOFF, Map.of()))),
                new ModelStep.Reply(List.of("Claro, le aviso a alguien de la clínica.")));

        AgentOutcome first = new ConversationAgent(notUnderstood, List.of()).run(context, "instrucciones", transcript);
        AgentOutcome second = new ConversationAgent(handoff, List.of()).run(context, "instrucciones", transcript);

        assertTrue(first.notUnderstood());
        assertFalse(first.handoff());
        assertTrue(second.handoff());
    }

    @Test
    void theOptionsOfTheLastToolThatOfferedSomeAreOffered() {
        List<ConversationOption> slots = List.of(new ConversationOption("slot:a", "Jue 16:00"), new ConversationOption("slot:b", "Jue 16:30"));
        RecordingTool tool = new RecordingTool("buscar_horarios", ToolOutcome.of(Map.of("horarios", 2)).withOptions(slots));
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "buscar_horarios", Map.of()))),
                new ModelStep.Reply(List.of("Tengo estos horarios el jueves 👇")));

        AgentOutcome outcome = new ConversationAgent(model, List.of(tool)).run(context, "instrucciones", transcript);

        assertEquals(slots, outcome.options());
    }

    @Test
    void aVerbatimTextFromAToolGoesOutAsItsOwnLastBubbleWithItsButtons() {
        List<ConversationOption> buttons = List.of(new ConversationOption("consentimiento:acepto", "Acepto"),
                new ConversationOption("consentimiento:no", "No acepto"));
        String official = "¿Autoriza a la clínica a contactarle?\n\nAviso de privacidad: https://sonrisa.mx/privacidad";
        RecordingTool consent = new RecordingTool("pedir_consentimiento",
                ToolOutcome.of(Map.of("enviado", true)).withOptions(buttons).withVerbatim(official));
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "pedir_consentimiento", Map.of()))),
                new ModelStep.Reply(List.of("¡Con gusto te registro! Antes necesito tu autorización:")));

        AgentOutcome outcome = new ConversationAgent(model, List.of(consent)).run(context, "instrucciones", transcript);

        assertEquals(List.of("¡Con gusto te registro! Antes necesito tu autorización:", official), outcome.bubbles());
        assertEquals(buttons, outcome.options());
        assertFalse(outcome.handoff(), "el enlace del texto oficial no lo escribio el modelo");
    }

    // ---- guarda de datos ---------------------------------------------------------------------

    @Test
    void anInventedFigureGetsOneChanceToBeCorrected() {
        RecordingTool prices = new RecordingTool("servicios_y_precios",
                ToolOutcome.of(Map.of("precio_desde", "$650")).withFacts(List.of("$650")));
        ScriptedModel model = new ScriptedModel(
                new ModelStep.CallTools(List.of(new ToolCall("c1", "servicios_y_precios", Map.of()))),
                new ModelStep.Reply(List.of("La limpieza cuesta $700.")),
                new ModelStep.Reply(List.of("La limpieza está desde $650.")));

        AgentOutcome outcome = new ConversationAgent(model, List.of(prices)).run(context, "instrucciones", transcript);

        assertEquals(List.of("La limpieza está desde $650."), outcome.bubbles());
        AgentMessage note = model.transcripts.get(2).getLast();
        assertTrue(note instanceof AgentMessage.Note && ((AgentMessage.Note) note).text().contains("700"), note.toString());
    }

    @Test
    void ifItInventsAgainASafeMessageGoesOutAndItCountsAsAFailure() {
        ScriptedModel model = new ScriptedModel(
                new ModelStep.Reply(List.of("La limpieza cuesta $700.")),
                new ModelStep.Reply(List.of("Perdón, cuesta $800.")));

        AgentOutcome outcome = new ConversationAgent(model, List.of()).run(context, "instrucciones", transcript);

        assertEquals(List.of(ConversationAgent.UNVERIFIED_REPLY), outcome.bubbles());
        assertTrue(outcome.failed());
        assertFalse(outcome.handoff());
    }

    @Test
    void theDateTheCodeGaveCanBeMentioned() {
        ScriptedModel model = new ScriptedModel(new ModelStep.Reply(List.of("Hoy 27 de septiembre estamos cerrados a las 21:00.")));

        AgentOutcome outcome = new ConversationAgent(model, List.of())
                .run(context, "Hoy es domingo 27 de septiembre de 2026 y son las 21:00 en la clínica.", transcript);

        assertFalse(outcome.handoff(), outcome.toString());
    }

    // ---- dobles ------------------------------------------------------------------------------

    static final class ScriptedModel implements ConversationModelPort {
        final Deque<Object> steps = new ArrayDeque<>();
        final List<List<AgentMessage>> transcripts = new ArrayList<>();
        final List<List<ToolSpec>> tools = new ArrayList<>();

        ScriptedModel(Object... steps) {
            this.steps.addAll(List.of(steps));
        }

        @Override
        public ModelStep next(UUID clinicId, String systemInstruction, List<AgentMessage> transcript, List<ToolSpec> tools) {
            this.transcripts.add(List.copyOf(transcript));
            this.tools.add(List.copyOf(tools));
            Object step = steps.isEmpty() ? new ModelStep.Reply(List.of("…")) : steps.poll();
            if (step instanceof RuntimeException failure) throw failure;
            return (ModelStep) step;
        }
    }

    static final class RecordingTool implements AgentTool {
        private final String name;
        private final ToolOutcome outcome;
        ToolContext lastContext;

        RecordingTool(String name, ToolOutcome outcome) {
            this.name = name;
            this.outcome = outcome;
        }

        @Override public ToolSpec spec() { return new ToolSpec(name, "Herramienta de prueba", List.of()); }

        @Override public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
            lastContext = context;
            return outcome;
        }
    }
}
