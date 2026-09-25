package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.ports.out.DoctorIdentityPort;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autoriza cada suscripcion en tiempo real. Solo se reconocen los canales de
 * {@link com.jclinical.automation.domain.model.RealtimeDestinations} escritos exactamente; cualquier
 * otro destino se niega.
 */
public class RealtimeAccessPolicy {

    private static final String ID = "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";
    private static final Pattern CHATS = Pattern.compile("/topic/clinics/" + ID + "/chats");
    private static final Pattern DOCTOR_INBOX =
            Pattern.compile("/topic/clinics/" + ID + "/doctors/" + ID + "/appointment-requests");

    private final StaffPermissionCheckerPort permissions;
    private final DoctorIdentityPort identity;

    public RealtimeAccessPolicy(StaffPermissionCheckerPort permissions, DoctorIdentityPort identity) {
        this.permissions = permissions;
        this.identity = identity;
    }

    public boolean canSubscribe(UUID userId, String destination) {
        if (userId == null || destination == null) {
            return false;
        }
        Matcher chats = CHATS.matcher(destination);
        if (chats.matches()) {
            return permissions.hasPermission(UUID.fromString(chats.group(1)), userId, StaffPermission.VIEW_PATIENTS);
        }
        Matcher inbox = DOCTOR_INBOX.matcher(destination);
        if (inbox.matches()) {
            UUID doctorStaffId = UUID.fromString(inbox.group(2));
            return identity.doctorStaffIdOfUser(UUID.fromString(inbox.group(1)), userId)
                    .map(doctorStaffId::equals)
                    .orElse(false);
        }
        return false;
    }
}
