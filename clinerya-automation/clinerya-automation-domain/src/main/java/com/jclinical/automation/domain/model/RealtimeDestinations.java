package com.jclinical.automation.domain.model;

import java.util.UUID;

/**
 * Canales STOMP de la automatizacion. Los chats solo anuncian actividad (sin contenido); la bandeja es
 * de un medico en particular.
 */
public final class RealtimeDestinations {

    static final String CLINICS = "/topic/clinics/";

    private RealtimeDestinations() {
    }

    public static String chats(UUID clinicId) {
        return CLINICS + clinicId + "/chats";
    }

    public static String doctorInbox(UUID clinicId, UUID doctorStaffId) {
        return CLINICS + clinicId + "/doctors/" + doctorStaffId + "/appointment-requests";
    }
}
