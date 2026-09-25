package com.jclinical.automation.infra.adapters.in.messaging;

import com.jclinical.automation.domain.model.WhatsAppMessageReceivedEvent;
import com.jclinical.automation.domain.ports.in.ProcessInboundWhatsAppUseCase;
import com.jclinical.automation.infra.config.AutomationRabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Procesa en segundo plano los mensajes que el webhook acepto (conversacion + respuestas en cola). */
@Component
@RequiredArgsConstructor
@Slf4j
public class InboundWhatsAppMessageListener {

    private final ProcessInboundWhatsAppUseCase processor;

    @RabbitListener(queues = AutomationRabbitConfig.WHATSAPP_MESSAGE_RECEIVED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onMessage(WhatsAppMessageReceivedEvent event) {
        log.info(">>>> [AUTOMATIZACION] Mensaje de WhatsApp {} para la clinica {}", event.waMessageId(), event.clinicId());
        processor.process(event);
    }
}
