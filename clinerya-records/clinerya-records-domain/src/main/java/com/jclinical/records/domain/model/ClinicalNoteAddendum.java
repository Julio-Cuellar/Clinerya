package com.jclinical.records.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Nota de enmienda (addendum) sobre una nota clínica ya firmada. La NOM-004 prohíbe
 * borrar o modificar la nota original; la corrección se agrega como addendum aparte,
 * con su propio hash y firma en {@code records.document_signatures}. Nunca toca la
 * nota original.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalNoteAddendum {
    private UUID id;
    private UUID clinicalNoteId;
    private UUID patientId;
    private UUID clinicId;
    private String content;
    private UUID createdByUserId;
    private String createdByUserName;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
