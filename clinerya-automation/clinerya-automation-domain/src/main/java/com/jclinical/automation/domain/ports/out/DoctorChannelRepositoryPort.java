package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.DoctorChannel;

import java.util.Optional;
import java.util.UUID;

public interface DoctorChannelRepositoryPort {

    Optional<DoctorChannel> find(UUID clinicId, UUID staffId);

    /** Medico con avisos activos en ese celular (asi se reconoce cuando escribe por WhatsApp). */
    Optional<DoctorChannel> findActiveByPhone(UUID clinicId, String phone);

    DoctorChannel save(DoctorChannel channel);
}
