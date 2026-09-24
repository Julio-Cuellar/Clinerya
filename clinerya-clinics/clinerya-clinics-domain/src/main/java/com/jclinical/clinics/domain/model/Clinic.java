package com.jclinical.clinics.domain.model;

import com.jclinical.core.domain.ClinicSpecialty;
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
public class Clinic {
    private UUID id;
    private UUID organizationId;
    private UUID ownerUserId;
    private String name;
    private String legalName;
    private String rfc;
    private String taxRegimeCode;
    private String addressStreet;
    private String addressColonia;
    private String addressMunicipality;
    private String addressState;
    private String addressZip;
    private String phone;
    private String email;
    private String logoUrl;
    private String timezone;
    @Builder.Default
    private ClinicSpecialty specialty = ClinicSpecialty.SIN_CONFIGURAR;
    private String privacyNoticeUrl;
    private String cofeprisPermitNumber;
    private String responsibleDoctorName;
    private String responsibleDoctorProfessionalLicense;
    private LocalDateTime dataProcessorAgreedAt;
    private UUID legalRepresentativeStaffId;
    private boolean active;
    @Builder.Default
    private Integer materialReservationLeadDays = 3;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Métodos de negocio
    public void assignLegalRepresentative(UUID staffId) {
        if (staffId == null) {
            throw new IllegalArgumentException("El ID del representante legal no puede ser nulo");
        }
        this.legalRepresentativeStaffId = staffId;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTaxData(String legalName, String rfc, String taxRegimeCode) {
        this.legalName = legalName;
        this.rfc = rfc;
        this.taxRegimeCode = taxRegimeCode;
        this.updatedAt = LocalDateTime.now();
    }
}
