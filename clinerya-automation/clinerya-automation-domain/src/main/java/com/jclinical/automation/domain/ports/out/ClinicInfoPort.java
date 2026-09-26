package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.ClinicInfo;

import java.util.Optional;
import java.util.UUID;

/** Datos publicos y horario de la clinica, leidos de sus modulos (clinicas y agenda). */
public interface ClinicInfoPort {

    Optional<ClinicInfo> find(UUID clinicId);
}
