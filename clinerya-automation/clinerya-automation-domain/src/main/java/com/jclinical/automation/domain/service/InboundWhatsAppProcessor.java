package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.model.WhatsAppInboundMessage.Kind;
import com.jclinical.automation.domain.model.WhatsAppMessageReceivedEvent;
import com.jclinical.automation.domain.ports.in.HandleInboundMessageUseCase;
import com.jclinical.automation.domain.ports.in.ProcessInboundWhatsAppUseCase;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;

/**
 * Lleva a la conversacion un mensaje ya aceptado por el webhook y deja sus respuestas en la cola de
 * salida, en el mismo orden. Lo que no es texto ni una opcion recibe un aviso y no mueve nada.
 */
public class InboundWhatsAppProcessor implements ProcessInboundWhatsAppUseCase {

    static final String UNSUPPORTED_NOTICE =
            "Por ahora solo puedo leer mensajes de texto. ¿Me escribes tu mensaje, por favor?";

    private final HandleInboundMessageUseCase conversations;
    private final OutboundMessageQueuePort outbound;
    private final ChatHistoryPort history;

    public InboundWhatsAppProcessor(HandleInboundMessageUseCase conversations, OutboundMessageQueuePort outbound,
                                    ChatHistoryPort history) {
        this.conversations = conversations;
        this.outbound = outbound;
        this.history = history;
    }

    @Override
    public void process(WhatsAppMessageReceivedEvent event) {
        if (event.kind() == Kind.UNSUPPORTED) {
            outbound.enqueue(new PatientNotification(event.clinicId(), event.fromPhone(), OutboundReply.text(UNSUPPORTED_NOTICE)));
            return;
        }
        String selectedOptionId = event.kind() == Kind.OPTION ? event.selectedOptionId() : null;
        for (OutboundReply reply : conversations.handle(new InboundMessage(event.clinicId(), event.fromPhone(),
                event.text(), selectedOptionId, event.receivedAt()))) {
            outbound.enqueue(new PatientNotification(event.clinicId(), event.fromPhone(), reply));
        }
    }
}
