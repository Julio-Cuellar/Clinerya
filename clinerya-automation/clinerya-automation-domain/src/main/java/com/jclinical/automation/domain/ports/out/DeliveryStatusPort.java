package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.DeliveryStatusUpdate;

import java.util.UUID;

/** Guarda el estado de entrega de un mensaje enviado. Nunca retrocede (leido no vuelve a entregado). */
public interface DeliveryStatusPort {

    void record(UUID clinicId, DeliveryStatusUpdate update);
}
