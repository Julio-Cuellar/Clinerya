package com.jclinical.records.domain.ports.out;

import java.util.UUID;

/**
 * Sincroniza los datos clínicos tipados (alergias / padecimientos / medicación)
 * a partir de los campos de una plantilla de historia clínica marcados con
 * {@code clinicalMapping} en su {@code schemaJson}. Solo toca filas con
 * {@code source = TEMPLATE}; las capturadas a mano quedan intactas.
 * Un JSON mal formado no hace nada (no lanza).
 */
public interface TemplateClinicalDataSyncPort {
    void sync(UUID clinicId, UUID patientId, String schemaJson, String answersJson,
              UUID actingUserId, String actingUserName);
}
