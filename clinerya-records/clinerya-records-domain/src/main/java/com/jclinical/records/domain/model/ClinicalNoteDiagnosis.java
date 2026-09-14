package com.jclinical.records.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Diagnostico CIE-10 asociado a una nota clinica. Se captura al firmar la nota
 * (junto con SOAP y signos vitales) y queda incluido en el {@code documentHash};
 * la nota firmada nunca se modifica, asi que estos registros tampoco cambian despues.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalNoteDiagnosis {
    private UUID id;
    private UUID clinicalNoteId;
    private UUID clinicId;
    private String icd10Code;
    private DiagnosisKind kind;
    private LocalDateTime createdAt;
}
