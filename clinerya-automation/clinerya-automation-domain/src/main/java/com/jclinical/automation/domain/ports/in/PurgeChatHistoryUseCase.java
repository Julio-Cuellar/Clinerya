package com.jclinical.automation.domain.ports.in;

/** Borra los mensajes mas viejos que la retencion que configuro cada clinica (12 meses por defecto). */
public interface PurgeChatHistoryUseCase {

    /** @return cuantos mensajes se borraron. */
    int purgeExpired();
}
