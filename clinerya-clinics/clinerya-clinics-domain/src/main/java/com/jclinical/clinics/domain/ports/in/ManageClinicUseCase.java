package com.jclinical.clinics.domain.ports.in;

import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.clinics.domain.model.ResponsibleDoctorSetup;
import com.jclinical.core.domain.ClinicSpecialty;

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
            ResponsibleDoctorSetup responsibleDoctor
    );

    /**
     * Cierra el alta de una clinica recien registrada: guarda sus datos, su medico responsable y
     * si el titular atiende pacientes (con su cedula) o solo gestiona la cuenta. Solo el dueño.
     */
    Clinic completeSetup(UUID ownerUserId, UUID clinicId, ClinicSetupDetails details,
                         ResponsibleDoctorSetup responsibleDoctor);

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

    /**
     * Cambia el perfil de especialidad de la clínica. Sólo afecta presentación: las cotizaciones
     * ya registradas conservan sus datos, incluido el número de diente, aunque deje de mostrarse.
     */
    Clinic updateSpecialty(UUID actingUserId, UUID clinicId, ClinicSpecialty specialty);

    Clinic getClinic(UUID ownerUserId, UUID clinicId);

    List<Clinic> getClinicsByOwner(UUID ownerUserId);

    record ClinicSetupDetails(
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
            String logoUrl,
            String cofeprisPermitNumber
    ) {}
}
