package com.jclinical.clinics.domain.ports.in;

import java.util.Optional;
import java.util.UUID;

/**
 * Datos de la clinica que se le pueden dar a un paciente (por ejemplo, el asistente de WhatsApp a un
 * numero que aun no es paciente). Nada fiscal ni interno: solo lo que la clinica publicaria.
 */
public interface GetClinicPublicProfileUseCase {

    Optional<ClinicPublicProfile> getPublicProfile(UUID clinicId);

    /** {@code address}, {@code phone}, {@code email} y {@code privacyNoticeUrl} pueden faltar. */
    record ClinicPublicProfile(UUID clinicId, String name, String address, String phone, String email,
                               String privacyNoticeUrl) {}
}
