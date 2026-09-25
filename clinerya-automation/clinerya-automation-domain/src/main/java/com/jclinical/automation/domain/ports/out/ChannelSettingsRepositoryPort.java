package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.ChannelSettings;

import java.util.Optional;
import java.util.UUID;

public interface ChannelSettingsRepositoryPort {

    Optional<ChannelSettings> findByClinicId(UUID clinicId);

    Optional<ChannelSettings> findByPhoneNumberId(String phoneNumberId);

    Optional<ChannelSettings> findByWebhookKey(String webhookKey);

    ChannelSettings save(ChannelSettings settings);
}
