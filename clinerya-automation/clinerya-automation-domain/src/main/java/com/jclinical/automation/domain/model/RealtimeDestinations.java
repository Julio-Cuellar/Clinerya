package com.jclinical.automation.domain.model;

import java.util.UUID;

/** Canales STOMP de la automatizacion. */
public final class RealtimeDestinations {

    private RealtimeDestinations() {
    }

    public static String chats(UUID clinicId) {
        throw new UnsupportedOperationException("pendiente");
    }

    public static String doctorInbox(UUID clinicId, UUID doctorStaffId) {
        throw new UnsupportedOperationException("pendiente");
    }
}
