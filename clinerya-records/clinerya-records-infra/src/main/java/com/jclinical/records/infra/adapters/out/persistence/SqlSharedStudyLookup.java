package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.attachments.AttachmentEntity;
import com.jclinical.attachments.AttachmentService;
import com.jclinical.records.domain.ports.out.SharedStudyLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador del puerto de estudios compartidos sobre el módulo de adjuntos. Solo
 * lectura: no expone subida ni borrado, y confina cada consulta al paciente,
 * clínica y elemento del enlace.
 */
@Component
@RequiredArgsConstructor
public class SqlSharedStudyLookup implements SharedStudyLookupPort {

    private final AttachmentService attachmentService;

    @Override
    public List<SharedStudy> listStudies(UUID patientId, UUID clinicId, String elementId) {
        return attachmentService.listByPatientClinicAndElement(patientId, clinicId, elementId).stream()
                .map(SqlSharedStudyLookup::toSharedStudy)
                .toList();
    }

    @Override
    public Optional<SharedStudyContent> readStudy(UUID attachmentId, UUID patientId, UUID clinicId, String elementId) {
        AttachmentEntity attachment;
        try {
            attachment = attachmentService.get(attachmentId, patientId);
        } catch (IllegalArgumentException notFound) {
            return Optional.empty();
        }
        if (!clinicId.equals(attachment.getClinicId()) || !elementId.equals(attachment.getElementId())) {
            return Optional.empty();
        }
        byte[] bytes = attachmentService.readContent(attachment);
        return Optional.of(new SharedStudyContent(attachment.getOriginalFilename(), attachment.getContentType(), bytes));
    }

    private static SharedStudy toSharedStudy(AttachmentEntity entity) {
        return new SharedStudy(
                entity.getId(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getCreatedAt());
    }
}
