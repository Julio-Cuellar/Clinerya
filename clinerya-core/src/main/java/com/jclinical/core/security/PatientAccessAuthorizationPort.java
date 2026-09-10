package com.jclinical.core.security;

import java.util.UUID;

/**
 * Resuelve qué nivel de acceso tiene un usuario sobre el expediente de un paciente,
 * combinando su rol como personal de la clínica y los permisos otorgados a especialistas
 * externos.
 *
 * <p>Vive en {@code clinerya-core} —igual que {@link ClinicMembershipPort}— porque es un
 * contrato de seguridad transversal: lo consumen módulos que no deben depender entre sí
 * (expediente, tratamientos, adjuntos). La implementación la aporta el módulo que conoce
 * a la vez al personal y a las colaboraciones externas.
 */
public interface PatientAccessAuthorizationPort {

    AccessDecision resolveAccess(UUID requestingUserId, UUID clinicId, UUID patientId);

    enum AccessLevel { NONE, READ_ONLY, COMMENT, READ_WRITE }

    record AccessDecision(AccessLevel level, boolean viaExternalGrant) {}
}
