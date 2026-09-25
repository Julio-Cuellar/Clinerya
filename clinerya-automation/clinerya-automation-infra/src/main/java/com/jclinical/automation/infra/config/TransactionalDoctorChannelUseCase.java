package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.ports.in.ManageDoctorChannelUseCase;
import com.jclinical.automation.domain.service.DoctorChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalDoctorChannelUseCase implements ManageDoctorChannelUseCase {

    private final DoctorChannelService channels;

    @Override
    @Transactional(readOnly = true)
    public DoctorChannelView getMine(UUID actingUserId, UUID clinicId) {
        return channels.getMine(actingUserId, clinicId);
    }

    @Override
    @Transactional
    public DoctorChannelView updateMine(UUID actingUserId, UUID clinicId, DoctorChannelUpdate update) {
        return channels.updateMine(actingUserId, clinicId, update);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorChannelView get(UUID actingUserId, UUID clinicId, UUID staffId) {
        return channels.get(actingUserId, clinicId, staffId);
    }

    @Override
    @Transactional
    public DoctorChannelView update(UUID actingUserId, UUID clinicId, UUID staffId, DoctorChannelUpdate update) {
        return channels.update(actingUserId, clinicId, staffId, update);
    }
}
