package com.jclinical.patients.domain.ports.in;

import java.util.UUID;

public interface DeletePatientUseCase {
    boolean deletePatient(UUID id);
}
