package com.jclinical.users.infra.adapters.in.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DebugEventListener {

    @EventListener
    public void handleAnyEvent(Object event) {
        if (event.getClass().getPackageName().startsWith("com.jclinical")) {
            log.info("<<<< [BUS DE EVENTOS] Recibido: {} - Detalles: {}", 
                     event.getClass().getSimpleName(), event);
        }
    }
}
