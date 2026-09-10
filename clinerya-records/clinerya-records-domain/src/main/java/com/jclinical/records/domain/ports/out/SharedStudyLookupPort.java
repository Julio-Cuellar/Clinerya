package com.jclinical.records.domain.ports.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso de solo lectura a los estudios (adjuntos) de un expediente, para
 * exponerlos a través de un enlace compartido. Lo implementa el módulo de
 * adjuntos; el dominio de expedientes no conoce su modelo.
 */
public interface SharedStudyLookupPort {

    List<SharedStudy> listStudies(UUID patientId, UUID clinicId, String elementId);

    /**
     * Devuelve el contenido de un estudio si pertenece a ese paciente, clínica y
     * elemento; vacío si no existe o no corresponde.
     */
    Optional<SharedStudyContent> readStudy(UUID attachmentId, UUID patientId, UUID clinicId, String elementId);

    record SharedStudy(
            UUID id,
            String filename,
            String contentType,
            long sizeBytes,
            LocalDateTime createdAt
    ) {}

    record SharedStudyContent(
            String filename,
            String contentType,
            byte[] bytes
    ) {}
}
