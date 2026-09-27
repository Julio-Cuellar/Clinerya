package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.agent.AgentMessage.ToolCall;
import com.jclinical.automation.domain.agent.AgentMessage.ToolResult;
import com.jclinical.automation.domain.model.ConversationOption;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Un turno del agente: el modelo propone contestar o usar herramientas; el codigo ejecuta las
 * herramientas con el contexto que el mismo arma, limita los pasos y, si el modelo no responde
 * (tras un reintento silencioso), contesta con honestidad y pide que lo atienda una persona.
 */
public final class ConversationAgent {

    public static final String NOT_UNDERSTOOD = "no_entendi";
    public static final String HANDOFF = "pasar_a_persona";
    public static final int MAX_STEPS = 6;
    public static final int MAX_BUBBLES = 3;
    public static final String UNAVAILABLE_REPLY = "Perdona, en este momento no puedo responderte bien. "
            + "Ya le avisé a alguien de la clínica para que te atienda por este mismo chat.";

    /** Una falla se reintenta en silencio; la segunda ya se le dice al paciente. */
    private static final int MAX_FAILURES = 2;
    private static final List<ToolSpec> BUILT_IN = List.of(
            new ToolSpec(NOT_UNDERSTOOD, "Úsala cuando no entiendas lo que pide el paciente, antes de pedirle con "
                    + "naturalidad que te lo explique de otra forma.", List.of()),
            new ToolSpec(HANDOFF, "Úsala cuando el paciente pida hablar con una persona o cuando no puedas ayudarle; "
                    + "después avísale que alguien de la clínica le responderá por este mismo chat.", List.of()));

    private final ConversationModelPort model;
    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public ConversationAgent(ConversationModelPort model, List<AgentTool> tools) {
        this.model = model;
        tools.forEach(tool -> this.tools.put(tool.spec().name(), tool));
    }

    public AgentOutcome run(ToolContext context, String systemInstruction, List<AgentMessage> transcript) {
        List<AgentMessage> working = new ArrayList<>(transcript);
        List<ToolSpec> specs = specs();
        List<ConversationOption> options = List.of();
        boolean notUnderstood = false;
        boolean handoff = false;
        int failures = 0;
        for (int step = 0; step < MAX_STEPS; step++) {
            ModelStep next = ask(context, systemInstruction, working, specs);
            if (next instanceof ModelStep.CallTools call && !call.calls().isEmpty()) {
                for (ToolCall toolCall : call.calls()) {
                    working.add(toolCall);
                    if (NOT_UNDERSTOOD.equals(toolCall.name())) {
                        notUnderstood = true;
                        working.add(new ToolResult(toolCall.id(), toolCall.name(), Map.of("ok", true)));
                    } else if (HANDOFF.equals(toolCall.name())) {
                        handoff = true;
                        working.add(new ToolResult(toolCall.id(), toolCall.name(), Map.of("ok", true)));
                    } else {
                        ToolOutcome outcome = execute(context, toolCall);
                        if (!outcome.options().isEmpty()) {
                            options = outcome.options();
                        }
                        working.add(new ToolResult(toolCall.id(), toolCall.name(), outcome.content()));
                    }
                }
                continue;
            }
            List<String> bubbles = next instanceof ModelStep.Reply reply ? bubbles(reply.bubbles()) : List.of();
            if (!bubbles.isEmpty()) {
                return new AgentOutcome(bubbles, options, notUnderstood, handoff, false);
            }
            if (++failures >= MAX_FAILURES) {
                return unavailable();
            }
        }
        return unavailable();
    }

    private ModelStep ask(ToolContext context, String systemInstruction, List<AgentMessage> working, List<ToolSpec> specs) {
        try {
            return model.next(context.clinicId(), systemInstruction, List.copyOf(working), specs);
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private ToolOutcome execute(ToolContext context, ToolCall call) {
        AgentTool tool = tools.get(call.name());
        if (tool == null) {
            return ToolOutcome.of(Map.of("error", "Herramienta desconocida: " + call.name()));
        }
        try {
            ToolOutcome outcome = tool.run(context, call.arguments());
            return outcome == null ? ToolOutcome.of(Map.of("error", "Sin resultado.")) : outcome;
        } catch (RuntimeException failure) {
            // El detalle tecnico no viaja al modelo (ni, por el, al paciente).
            return ToolOutcome.of(Map.of("error", "No se pudo completar la consulta en este momento."));
        }
    }

    private List<ToolSpec> specs() {
        List<ToolSpec> all = new ArrayList<>();
        tools.values().forEach(tool -> all.add(tool.spec()));
        all.addAll(BUILT_IN);
        return List.copyOf(all);
    }

    /** Sin burbujas vacias y como maximo 3: lo que sobra se une a la ultima. */
    private static List<String> bubbles(List<String> proposed) {
        List<String> clean = proposed.stream().filter(text -> text != null && !text.isBlank()).map(String::trim).toList();
        if (clean.size() <= MAX_BUBBLES) {
            return clean;
        }
        List<String> capped = new ArrayList<>(clean.subList(0, MAX_BUBBLES - 1));
        capped.add(String.join("\n\n", clean.subList(MAX_BUBBLES - 1, clean.size())));
        return List.copyOf(capped);
    }

    private static AgentOutcome unavailable() {
        return new AgentOutcome(List.of(UNAVAILABLE_REPLY), List.of(), false, true, true);
    }
}
