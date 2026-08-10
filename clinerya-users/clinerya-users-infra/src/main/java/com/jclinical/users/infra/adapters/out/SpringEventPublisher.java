package com.jclinical.users.infra.adapters.out;

import com.jclinical.users.domain.ports.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringEventPublisher implements EventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(Object event) {
        if (event != null) {
            log.info(">>>> [BUS DE EVENTOS] Publicando evento: {} - Detalles: {}", 
                     event.getClass().getSimpleName(), event);
            applicationEventPublisher.publishEvent(event);
        }
    }
}
