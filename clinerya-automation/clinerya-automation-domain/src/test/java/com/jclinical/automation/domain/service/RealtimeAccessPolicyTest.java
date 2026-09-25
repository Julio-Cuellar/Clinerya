package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.RealtimeDestinations;
import com.jclinical.core.security.StaffPermission;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quien puede suscribirse a que canal en tiempo real. Todo lo que no se reconoce se niega; los chats
 * solo avisan de actividad (el contenido se lee por el endpoint auditado).
 */
class RealtimeAccessPolicyTest {

    private final UUID clinicId = UUID.randomUUID();
    private final UUID otherClinicId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID doctorUserId = UUID.randomUUID();
    private final UUID otherDoctorId = UUID.randomUUID();
    private final UUID receptionUserId = UUID.randomUUID();
    private final UUID strangerUserId = UUID.randomUUID();

    private final Set<UUID> canSeePatients = Set.of(doctorUserId, receptionUserId);
    private final Map<UUID, UUID> doctorOfUser = Map.of(doctorUserId, doctorId);

    private final RealtimeAccessPolicy policy = new RealtimeAccessPolicy(
            (clinic, user, permission) -> clinic.equals(clinicId) && permission == StaffPermission.VIEW_PATIENTS
                    && canSeePatients.contains(user),
            (clinic, user) -> clinic.equals(clinicId) ? Optional.ofNullable(doctorOfUser.get(user)) : Optional.empty());

    @Test
    void destinationsHaveAStableShape() {
        assertEquals("/topic/clinics/" + clinicId + "/chats", RealtimeDestinations.chats(clinicId));
        assertEquals("/topic/clinics/" + clinicId + "/doctors/" + doctorId + "/appointment-requests",
                RealtimeDestinations.doctorInbox(clinicId, doctorId));
    }

    @Test
    void chatActivityIsForStaffWhoCanSeePatients() {
        assertTrue(policy.canSubscribe(doctorUserId, RealtimeDestinations.chats(clinicId)));
        assertTrue(policy.canSubscribe(receptionUserId, RealtimeDestinations.chats(clinicId)));
        assertFalse(policy.canSubscribe(strangerUserId, RealtimeDestinations.chats(clinicId)));
        assertFalse(policy.canSubscribe(doctorUserId, RealtimeDestinations.chats(otherClinicId)), "otra clinica");
    }

    @Test
    void theInboxIsOnlyForItsDoctor() {
        assertTrue(policy.canSubscribe(doctorUserId, RealtimeDestinations.doctorInbox(clinicId, doctorId)));
        assertFalse(policy.canSubscribe(doctorUserId, RealtimeDestinations.doctorInbox(clinicId, otherDoctorId)));
        assertFalse(policy.canSubscribe(receptionUserId, RealtimeDestinations.doctorInbox(clinicId, doctorId)));
        assertFalse(policy.canSubscribe(doctorUserId, RealtimeDestinations.doctorInbox(otherClinicId, doctorId)));
    }

    @Test
    void anythingElseIsDenied() {
        String base = "/topic/clinics/" + clinicId;
        for (String destination : new String[] {
                null, "", "/topic/clinics", base, base + "/chats/", base + "/chats/5215512345678",
                base + "/chats/../doctors/" + doctorId + "/appointment-requests",
                base + "/doctors/" + doctorId, "/topic/clinics/no-es-uuid/chats",
                "/topic/clinics/" + clinicId.toString().toUpperCase() + "/chats-x", "/user/queue/appointment-requests",
                "/queue/anything", "/app/chats"}) {
            assertFalse(policy.canSubscribe(doctorUserId, destination), String.valueOf(destination));
        }
    }

    @Test
    void anAnonymousSessionCannotSubscribe() {
        assertFalse(policy.canSubscribe(null, RealtimeDestinations.chats(clinicId)));
    }
}
