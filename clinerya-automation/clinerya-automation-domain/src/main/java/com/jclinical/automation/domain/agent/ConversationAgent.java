package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.agent.AgentMessage.ToolCall;
import com.jclinical.automation.domain.agent.AgentMessage.ToolResult;
import com.jclinical.automation.domain.model.ConversationOption;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Un turno del agente: el modelo propone contestar o usar herramientas; el codigo ejecuta las
 * herramientas con el contexto que el mismo arma y limita los pasos. Si el modelo no responde (tras un
 * reintento; despues de una herramienta, con un empujon para que conteste con su resultado), sale lo que
 * la herramienta dejo escrito, las opciones ya ofrecidas o se le pide al paciente que lo repita. Una falla
 * no pasa el chat a una persona: cuenta para la regla de 3 de la conversacion.
 */
public final class ConversationAgent {

    public static final String NOT_UNDERSTOOD = "no_entendi";
    public static final String HANDOFF = "pasar_a_persona";
    public static final int MAX_STEPS = 6;
    public static final int MAX_BUBBLES = 3;
    public static final String UNAVAILABLE_REPLY = "Perdona, se me cortó la respuesta. ¿Me lo repites, por favor?";
    public static final String UNVERIFIED_REPLY = "Perdona, prefiero no darte un dato que no pude confirmar. "
            + "¿Me lo preguntas de otra forma?";
    public static final String OPTIONS_REPLY = "Te dejo las opciones para que elijas:";
    static final String NUDGE = "Ya tienes el resultado de las herramientas. Responde ahora al paciente con un mensaje "
            + "breve basado en él.";
    static final String MISSING_LIST = "Tu respuesta anuncia una lista, pero en este turno no consultaste ninguna herramienta "
            + "que la dé, así que el paciente no vería nada. Consulta la herramienta que corresponde (por ejemplo "
            + "buscar_horarios) y luego responde; las opciones aparecen solas.";
    static final String BOOKING_CLAIM_NOTE = "No digas que la cita quedó agendada: la agenda solo la aparta cuando el médico "
            + "acepta la solicitud. Pide que confirme el horario; al confirmar, di que enviaste la solicitud al médico y que "
            + "le avisaremos en cuanto responda.";
    /** "ha quedado agendada", "ya está reservada", "te agendé": solo el medico agenda al aceptar la solicitud. */
    private static final Pattern BOOKING_CLAIM = Pattern.compile(
            "(?iu)\\b(?:qued[oó]|ha\\s+quedado|est[aá]|fue)\\s+(?:ya\\s+)?(?:agendad|reservad|apartad)[ao]\\b"
                    + "|\\bte\\s+(?:agend[eé]|reserv[eé])\\b");

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
        List<String> facts = new ArrayList<>();
        String verbatim = null;
        List<ConversationOption> verbatimOptions = List.of();
        String fallback = null;
        String closing = null;
        List<ConversationOption> fallbackOptions = List.of();
        boolean notUnderstood = false;
        boolean handoff = false;
        boolean corrected = false;
        int failures = 0;
        for (int step = 0; step < MAX_STEPS; step++) {
            ModelStep next = ask(context, systemInstruction, working, specs);
            if (next instanceof ModelStep.CallTools call && !call.calls().isEmpty()) {
                // Las llamadas viajan juntas y despues sus resultados juntos: asi lo espera el protocolo de Gemini.
                working.addAll(call.calls());
                for (ToolCall toolCall : call.calls()) {
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
                        facts.addAll(outcome.facts());
                        if (outcome.closing() != null && !outcome.closing().isBlank()) {
                            closing = outcome.closing();
                        }
                        if (outcome.verbatim() != null && !outcome.verbatim().isBlank()) {
                            verbatim = outcome.verbatim();
                            verbatimOptions = outcome.options();
                        }
                        if (outcome.fallback() != null && !outcome.fallback().isBlank()) {
                            fallback = outcome.fallback();
                            fallbackOptions = outcome.options();
                        }
                        working.add(new ToolResult(toolCall.id(), toolCall.name(), outcome.content()));
                    }
                }
                continue;
            }
            List<String> bubbles = next instanceof ModelStep.Reply reply ? bubbles(reply.bubbles()) : List.of();
            if (!bubbles.isEmpty() && closing != null) {
                return new AgentOutcome(List.of(closing), List.of(), notUnderstood, handoff, false);
            }
            if (!bubbles.isEmpty()) {
                List<String> invented = GroundingGuard.inventedFigures(String.join("\n", bubbles),
                        trusted(systemInstruction, working, facts), patientSaid(working));
                String problem = problemWith(bubbles, invented, verbatim == null ? options : verbatimOptions);
                if (problem == null) {
                    if (verbatim == null) {
                        return new AgentOutcome(bubbles, options, notUnderstood, handoff, false);
                    }
                    // El texto oficial lo escribe el codigo: va tal cual como ultima burbuja, con sus botones.
                    List<String> withVerbatim = new ArrayList<>(bubbles);
                    withVerbatim.add(verbatim);
                    return new AgentOutcome(withVerbatim, verbatimOptions, notUnderstood, handoff, false);
                }
                if (corrected) {
                    return !invented.isEmpty() && fallback == null
                            ? new AgentOutcome(List.of(UNVERIFIED_REPLY), List.of(), notUnderstood, handoff, true)
                            : giveUp(closing, fallback, fallbackOptions, options, notUnderstood, handoff);
                }
                corrected = true;
                working.add(new AgentMessage.Note(problem));
                continue;
            }
            if (++failures >= MAX_FAILURES) {
                return giveUp(closing, fallback, fallbackOptions, options, notUnderstood, handoff);
            }
            if (!working.isEmpty() && working.getLast() instanceof ToolResult) {
                working.add(new AgentMessage.Note(NUDGE));
            }
        }
        return giveUp(closing, fallback, fallbackOptions, options, notUnderstood, handoff);
    }

    /** Que corregirle a una respuesta antes de mandarla; null si se puede mandar. */
    private static String problemWith(List<String> bubbles, List<String> invented, List<ConversationOption> outgoing) {
        if (!invented.isEmpty()) {
            return "Tu respuesta incluía datos que no vienen de lo que consultaste ni de la conversación: "
                    + String.join(", ", invented) + ". Vuelve a escribirla usando solo datos consultados; si no los "
                    + "tienes, consúltalos con una herramienta o di que lo verificas.";
        }
        if (outgoing.isEmpty() && bubbles.getLast().stripTrailing().endsWith(":")) {
            return MISSING_LIST;
        }
        if (BOOKING_CLAIM.matcher(String.join("\n", bubbles)).find()) {
            return BOOKING_CLAIM_NOTE;
        }
        return null;
    }

    /** El modelo dejo de responder: lo que ya se hizo se dice, lo ya ofrecido se muestra; si no, que lo repita. */
    private static AgentOutcome giveUp(String closing, String fallback, List<ConversationOption> fallbackOptions,
                                       List<ConversationOption> options, boolean notUnderstood, boolean handoff) {
        if (closing != null) {
            return new AgentOutcome(List.of(closing), List.of(), notUnderstood, handoff, false);
        }
        if (fallback != null) {
            return new AgentOutcome(List.of(fallback), fallbackOptions, notUnderstood, handoff, false);
        }
        if (!options.isEmpty()) {
            return new AgentOutcome(List.of(OPTIONS_REPLY), options, notUnderstood, handoff, true);
        }
        return new AgentOutcome(List.of(UNAVAILABLE_REPLY), List.of(), notUnderstood, handoff, true);
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

    /** Fuentes de confianza: la instruccion (fecha, clinica), lo que ya dijo la clinica y lo consultado. */
    private static List<String> trusted(String systemInstruction, List<AgentMessage> working, List<String> facts) {
        List<String> sources = new ArrayList<>(facts);
        sources.add(systemInstruction);
        for (AgentMessage message : working) {
            if (message instanceof AgentMessage.Assistant assistant) {
                sources.add(assistant.text());
            } else if (message instanceof ToolResult result) {
                sources.add(String.valueOf(result.content()));
            }
        }
        return sources;
    }

    private static List<String> patientSaid(List<AgentMessage> working) {
        return working.stream().filter(AgentMessage.User.class::isInstance).map(message -> ((AgentMessage.User) message).text()).toList();
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

}
