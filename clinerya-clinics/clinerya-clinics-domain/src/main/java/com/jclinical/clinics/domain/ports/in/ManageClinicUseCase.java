package com.jclinical.clinics.domain.ports.in;

import com.jclinical.clinics.domain.model.Clinic;

import java.util.List;
import java.util.UUID;

public interface ManageClinicUseCase {
    
    Clinic createClinic(
            UUID ownerUserId,
            String name,
            String email,
            String timezone,
            String legalName,
            String rfc,
            String taxRegimeCode,
            String addressStreet,
            String addressColonia,
            String addressMunicipality,
            String addressState,
            String addressZip,
            String phone,
            String cofeprisPermitNumber,
            String responsibleDoctorName,
            String responsibleDoctorProfessionalLicense
    );

    Clinic updateClinic(
            UUID ownerUserId,
            UUID clinicId,
            String name,
            String legalName,
            String rfc,
            String taxRegimeCode,
            String addressStreet,
            String addressColonia,
            String addressMunicipality,
            String addressState,
            String addressZip,
            String phone,
            String email,
            String logoUrl,
            String timezone,
            String privacyNoticeUrl,
            String cofeprisPermitNumber,
            String responsibleDoctorName,
            String responsibleDoctorProfessionalLicense,
            Integer materialReservationLeadDays
    );

    Clinic getClinic(UUID ownerUserId, UUID clinicId);

    List<Clinic> getClinicsByOwner(UUID ownerUserId);
}
