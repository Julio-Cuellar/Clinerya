package com.jclinical.integrations.domain.ports.out;

import java.util.UUID;

public interface StateCodecPort {

    String encode(UUID clinicId, UUID staffId, boolean importPastEvents);

    DecodedState decode(String state);

    record DecodedState(UUID clinicId, UUID staffId, boolean importPastEvents) {}
}

