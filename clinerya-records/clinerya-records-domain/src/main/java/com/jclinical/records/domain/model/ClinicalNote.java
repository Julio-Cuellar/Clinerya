package com.jclinical.records.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalNote {
    private UUID id;
    private UUID patientId;
    private UUID clinicId;
    private UUID doctorId;
    private String subjective;
    private String objective;
    private VitalSigns vitalSigns;
    private String assessment;
    private String plan;
    private NoteStatus status;
    private UUID authoredByExternalUserId;
    private LocalDateTime signedAt;
    private UUID signedByUserId;
    private String documentHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isSigned() {
        return this.status == NoteStatus.SIGNED;
    }

    public void sign(UUID signedByUserId, LocalDateTime signedAt, String documentHash) {
        if (isSigned()) {
            throw new IllegalStateException("La nota ya ha sido firmada y no se puede volver a firmar.");
        }
        if (signedByUserId == null) {
            throw new IllegalArgumentException("El usuario firmante es obligatorio.");
        }
        if (documentHash == null || documentHash.isBlank()) {
            throw new IllegalArgumentException("El hash del documento firmado es obligatorio.");
        }
        this.status = NoteStatus.SIGNED;
        this.signedByUserId = signedByUserId;
        this.signedAt = signedAt == null ? LocalDateTime.now() : signedAt;
        this.documentHash = documentHash;
        this.updatedAt = this.signedAt;
    }

    public void update(String subjective, String objective, VitalSigns vitalSigns, String assessment, String plan) {
        if (isSigned()) {
            throw new IllegalStateException("No se puede modificar una nota clínica que ya ha sido firmada.");
        }
        this.subjective = subjective;
        this.objective = objective;
        this.vitalSigns = vitalSigns;
        this.assessment = assessment;
        this.plan = plan;
        this.updatedAt = LocalDateTime.now();
    }
}
