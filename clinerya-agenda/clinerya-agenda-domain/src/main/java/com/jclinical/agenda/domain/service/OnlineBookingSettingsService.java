package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.ports.in.ManageOnlineBookingSettingsUseCase;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsPort;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsRepositoryPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.util.UUID;

/**
 * Reglas de la reserva en linea. La clinica define la duracion de cada cupo; cada medico su
 * anticipacion minima. Sin configurar aplican los valores por defecto de {@link OnlineBookingSettingsPort}.
 */
public class OnlineBookingSettingsService implements ManageOnlineBookingSettingsUseCase, OnlineBookingSettingsPort {

    static final int MIN_SLOT_MINUTES = 10;
    static final int MAX_SLOT_MINUTES = 240;
    static final int SLOT_STEP_MINUTES = 5;
    static final int MAX_LEAD_MINUTES = 7 * 24 * 60;

    private final OnlineBookingSettingsRepositoryPort repository;
    private final StaffValidatorPort staffValidator;
    private final StaffPermissionCheckerPort permissionChecker;

    public OnlineBookingSettingsService(OnlineBookingSettingsRepositoryPort repository, StaffValidatorPort staffValidator,
                                        StaffPermissionCheckerPort permissionChecker) {
        this.repository = repository;
        this.staffValidator = staffValidator;
        this.permissionChecker = permissionChecker;
    }

    @Override
    public OnlineBookingSettings getSettings(UUID actingUserId, UUID clinicId) {
        require(clinicId, actingUserId, StaffPermission.VIEW_AGENDA, "No tienes permiso para ver la agenda de esta clínica.");
        return new OnlineBookingSettings(slotMinutes(clinicId), staffValidator.listActiveDoctors(clinicId).stream()
                .map(doctor -> new DoctorLeadTime(doctor.staffId(), doctor.fullName(),
                        minLeadMinutes(clinicId, doctor.staffId())))
                .toList());
    }

    @Override
    public void updateSlotMinutes(UUID actingUserId, UUID clinicId, int minutes) {
        require(clinicId, actingUserId, StaffPermission.MANAGE_AGENDA,
                "Solo quien administra la agenda puede cambiar la duración de los cupos.");
        if (minutes < MIN_SLOT_MINUTES || minutes > MAX_SLOT_MINUTES || minutes % SLOT_STEP_MINUTES != 0) {
            throw new IllegalArgumentException("La duración del cupo debe estar entre " + MIN_SLOT_MINUTES + " y "
                    + MAX_SLOT_MINUTES + " minutos, en múltiplos de " + SLOT_STEP_MINUTES + ".");
        }
        repository.saveSlotMinutes(clinicId, minutes);
    }

    @Override
    public void updateDoctorLeadMinutes(UUID actingUserId, UUID clinicId, UUID doctorStaffId, int minutes) {
        if (staffValidator.findActiveDoctor(doctorStaffId, clinicId).isEmpty()) {
            throw new IllegalArgumentException("El médico no atiende en esta clínica.");
        }
        boolean isThatDoctor = actingUserId != null
                && staffValidator.userIdOfStaff(doctorStaffId, clinicId).filter(actingUserId::equals).isPresent();
        if (!isThatDoctor) {
            require(clinicId, actingUserId, StaffPermission.MANAGE_AGENDA,
                    "Solo el propio médico o quien administra la agenda puede cambiar su anticipación mínima.");
        }
        if (minutes < 0 || minutes > MAX_LEAD_MINUTES) {
            throw new IllegalArgumentException("La anticipación mínima debe estar entre 0 minutos y 7 días.");
        }
        repository.saveLeadMinutes(clinicId, doctorStaffId, minutes);
    }

    @Override
    public int slotMinutes(UUID clinicId) {
        return repository.findSlotMinutes(clinicId).orElse(DEFAULT_SLOT_MINUTES);
    }

    @Override
    public int minLeadMinutes(UUID clinicId, UUID doctorStaffId) {
        return repository.findLeadMinutes(clinicId, doctorStaffId).orElse(DEFAULT_MIN_LEAD_MINUTES);
    }

    private void require(UUID clinicId, UUID actingUserId, StaffPermission permission, String deniedMessage) {
        if (actingUserId == null || !permissionChecker.hasPermission(clinicId, actingUserId, permission)) {
            throw new ClinicAccessDeniedException(deniedMessage);
        }
    }
}
