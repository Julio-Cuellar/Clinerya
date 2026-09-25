package com.jclinical.automation.infra.adapters.in.messaging;

import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent;
import com.jclinical.automation.infra.config.AutomationRabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentRequestResolvedListener {

    private final RequestOutcomeProcessor processor;

    @RabbitListener(queues = AutomationRabbitConfig.REQUEST_RESOLVED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onRequestResolved(AppointmentRequestResolvedEvent event) {
        log.info(">>>> [AUTOMATIZACION] Solicitud {} resuelta por el medico: {}", event.requestId(), event.outcome());
        processor.process(event);
    }
}
