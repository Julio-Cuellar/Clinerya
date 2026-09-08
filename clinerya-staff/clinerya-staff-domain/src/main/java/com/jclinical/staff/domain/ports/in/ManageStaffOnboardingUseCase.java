package com.jclinical.staff.domain.ports.in;

import com.jclinical.staff.domain.ports.in.ManageStaffCompensationUseCase.CompensationInput;

import java.util.UUID;

/**
 * Datos de nomina que el admin adjunta a una invitacion de personal ya creada, y
 * su traspaso al empleado cuando confirma. No participa en la creacion del
 * empleado en si (eso lo hace {@link ManageClinicStaffUseCase}).
 */
public interface ManageStaffOnboardingUseCase {

    /** Adjunta / reemplaza la compensacion de una invitacion. Exige MANAGE_PAYROLL. */
    void setInvitationCompensation(UUID clinicId, UUID actingUserId, UUID invitationId, CompensationInput compensation);

    /**
     * Copia la compensacion adjunta a la invitacion hacia el registro definitivo
     * del empleado recien confirmado y limpia la tabla puente. Sin verificacion de
     * permiso: paso interno del flujo de confirmacion (autenticado por token).
     */
    void applyInvitationCompensation(UUID invitationId, UUID staffId, UUID clinicId);
}
