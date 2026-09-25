package com.jclinical.automation.domain.ports.in;

/** Envia por WhatsApp el siguiente mensaje de la cola que ya toca enviar. */
public interface DispatchOutboundMessagesUseCase {

    /** @return false si no habia nada que enviar. */
    boolean dispatchNext();
}
