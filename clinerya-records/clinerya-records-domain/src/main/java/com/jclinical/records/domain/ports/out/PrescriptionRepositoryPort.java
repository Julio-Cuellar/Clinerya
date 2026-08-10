package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.Prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepositoryPort {
    Prescription save(Prescription prescription);
    List<Prescription> findByClinicIdAndPatientId(UUID clinicId, UUID patientId);
    Optional<Prescription> findById(UUID clinicId, UUID prescriptionId);
}
