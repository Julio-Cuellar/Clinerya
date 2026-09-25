package com.jclinical.agenda.infra.adapters.in.web;

import com.jclinical.agenda.domain.ports.in.ManageOnlineBookingSettingsUseCase;
import com.jclinical.agenda.domain.ports.in.ManageOnlineBookingSettingsUseCase.OnlineBookingSettings;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Reglas de la reserva en linea: duracion del cupo (clinica) y anticipacion minima (cada medico). */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/online-booking/settings")
@RequiredArgsConstructor
public class OnlineBookingSettingsController {

    private final ManageOnlineBookingSettingsUseCase settingsUseCase;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public ResponseEntity<OnlineBookingSettings> getSettings(@PathVariable UUID clinicId) {
        return ResponseEntity.ok(settingsUseCase.getSettings(currentUserResolver.getCurrentUserId(), clinicId));
    }

    @PutMapping("/slot-minutes")
    public ResponseEntity<OnlineBookingSettings> updateSlotMinutes(@PathVariable UUID clinicId,
                                                                   @RequestBody MinutesRequest request) {
        UUID userId = currentUserResolver.getCurrentUserId();
        settingsUseCase.updateSlotMinutes(userId, clinicId, request.minutes());
        return ResponseEntity.ok(settingsUseCase.getSettings(userId, clinicId));
    }

    @PutMapping("/doctors/{doctorStaffId}/lead-minutes")
    public ResponseEntity<OnlineBookingSettings> updateDoctorLeadMinutes(@PathVariable UUID clinicId,
                                                                         @PathVariable UUID doctorStaffId,
                                                                         @RequestBody MinutesRequest request) {
        UUID userId = currentUserResolver.getCurrentUserId();
        settingsUseCase.updateDoctorLeadMinutes(userId, clinicId, doctorStaffId, request.minutes());
        return ResponseEntity.ok(settingsUseCase.getSettings(userId, clinicId));
    }

    public record MinutesRequest(int minutes) {}
}
