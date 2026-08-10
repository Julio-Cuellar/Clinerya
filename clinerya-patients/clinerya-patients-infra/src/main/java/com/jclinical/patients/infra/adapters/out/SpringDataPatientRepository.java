package com.jclinical.patients.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataPatientRepository extends JpaRepository<PatientEntity, UUID> {
    List<PatientEntity> findByClinicId(UUID clinicId);
    boolean existsByCurpAndClinicId(String curp, UUID clinicId);
}
