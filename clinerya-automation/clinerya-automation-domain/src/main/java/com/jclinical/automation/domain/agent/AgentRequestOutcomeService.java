package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.agent.tools.ChooseProposalTool;
import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.ports.in.HandleRequestOutcomeUseCase;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.service.SlotLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La respuesta del medico a una solicitud hecha por el agente. El mensaje sale de los datos del evento
 * (sin IA). Solo reacciona si la conversacion sigue esperando esa solicitud: el evento viaja por el
 * outbox y puede repetirse.
 */
public final class AgentRequestOutcomeService implements HandleRequestOutcomeUseCase {

    static final String NONE_LABEL = "Ninguno me funciona";
    private static final int MAX_PROPOSALS = 9;

    private final ConversationRepositoryPort conversations;

    public AgentRequestOutcomeService(ConversationRepositoryPort conversations) {
        this.conversations = conversations;
    }

    @Override
    public Optional<PatientNotification> onRequestResolved(AppointmentRequestResolvedEvent event) {
        Optional<Conversation> waiting = conversations.findById(event.conversationId())
                .filter(conversation -> event.requestId().equals(conversation.requestId()));
        if (waiting.isEmpty()) {
            return Optional.empty();
        }
        Conversation conversation = waiting.get();
        String doctor = event.doctorName() == null || event.doctorName().isBlank() ? "El médico" : event.doctorName();
        String label = event.start() == null ? "horario que pediste" : SlotLabel.of(event.start());
        OutboundReply reply = switch (event.outcome()) {
            case BOOKED -> {
                save(conversation, null, List.of());
                yield OutboundReply.text("¡Listo! " + doctor + " confirmó tu cita del " + label + ". ¡Te esperamos!");
            }
            case REJECTED -> {
                save(conversation, null, List.of());
                String reason = event.reason() == null || event.reason().isBlank() ? "" : " (" + event.reason().trim() + ")";
                yield OutboundReply.text(doctor + " no pudo confirmar tu cita del " + label + reason
                        + ". Si quieres, buscamos otro horario.");
            }
            case OPTIONS_PROPOSED -> {
                List<ConversationOption> options = new ArrayList<>(event.options().stream().limit(MAX_PROPOSALS)
                        .map(slot -> new ConversationOption(ChooseProposalTool.PREFIX + event.requestId() + "|" + slot.start()
                                + "|" + slot.end(), SlotLabel.of(slot.start())))
                        .toList());
                options.add(new ConversationOption(ChooseProposalTool.NONE, NONE_LABEL));
                save(conversation, event.requestId(), options);
                yield new OutboundReply(doctor + " no puede en ese horario, pero te propone estas opciones 👇", options);
            }
            case EXPIRED -> {
                save(conversation, null, List.of());
                yield OutboundReply.text("Tu solicitud del " + label + " venció sin respuesta. ¿Quieres que busquemos otro horario?");
            }
        };
        return Optional.of(new PatientNotification(conversation.clinicId(), conversation.phone(), reply));
    }

    private void save(Conversation conversation, UUID requestId, List<ConversationOption> options) {
        conversations.save(new Conversation(conversation.id(), conversation.clinicId(), conversation.phone(),
                conversation.state(), conversation.patientId(), conversation.patientName(), conversation.doctorStaffId(),
                conversation.doctorName(), requestId, options, conversation.unrecognizedCount(), conversation.createdAt(),
                conversation.lastActivityAt()));
    }
}
