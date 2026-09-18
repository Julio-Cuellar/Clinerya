package com.jclinical.treatments.domain.ports.out;

import com.jclinical.core.domain.ClinicSpecialty;

import java.util.Optional;
import java.util.UUID;

/**
 * Lee el perfil de especialidad de la clinica dueña de la cotizacion.
 *
 * <p>El modulo de tratamientos lo necesita para decidir si una partida puede llevar localizador
 * clinico: hoy solo las clinicas odontologicas aceptan numero de diente. Ocultar el campo en la
 * interfaz no basta, porque la API sigue siendo alcanzable por cualquier cliente.
 */
public interface ClinicSpecialtyPort {

    Optional<ClinicSpecialty> findByClinicId(UUID clinicId);
}
