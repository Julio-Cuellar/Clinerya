package com.jclinical.clinics.infra.adapters.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clinics", schema = "clinics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Column(name = "legal_name")
    private String legalName;

    private String rfc;

    @Column(name = "tax_regime_code")
    private String taxRegimeCode;

    @Column(name = "address_street")
    private String addressStreet;

    @Column(name = "address_colonia")
    private String addressColonia;

    @Column(name = "address_municipality")
    private String addressMunicipality;

    @Column(name = "address_state")
    private String addressState;

    @Column(name = "address_zip")
    private String addressZip;

    private String phone;

    private String email;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "privacy_notice_url")
    private String privacyNoticeUrl;

    @Column(name = "cofepris_permit_number")
    private String cofeprisPermitNumber;

    @Column(name = "responsible_doctor_name")
    private String responsibleDoctorName;

    @Column(name = "responsible_doctor_professional_license")
    private String responsibleDoctorProfessionalLicense;

    @Column(name = "data_processor_agreed_at")
    private LocalDateTime dataProcessorAgreedAt;

    @Column(name = "legal_representative_staff_id")
    private UUID legalRepresentativeStaffId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "material_reservation_lead_days")
    private Integer materialReservationLeadDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
