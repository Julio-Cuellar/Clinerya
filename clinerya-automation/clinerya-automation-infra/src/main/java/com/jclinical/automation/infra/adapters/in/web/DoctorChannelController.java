package com.jclinical.automation.infra.adapters.in.web;

import com.jclinical.automation.domain.ports.in.ManageDoctorChannelUseCase;
import com.jclinical.automation.domain.ports.in.ManageDoctorChannelUseCase.DoctorChannelUpdate;
import com.jclinical.automation.domain.ports.in.ManageDoctorChannelUseCase.DoctorChannelView;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Celular donde el medico recibe avisos de solicitudes (D8). "/me" es el del propio medico; por
 * {staffId} lo administra quien tiene MANAGE_INTEGRATIONS.
 */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/automation/doctor-channels")
@RequiredArgsConstructor
public class DoctorChannelController {

    private final ManageDoctorChannelUseCase channels;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/me")
    public DoctorChannelView getMine(@PathVariable UUID clinicId) {
        return channels.getMine(currentUserResolver.getCurrentUserId(), clinicId);
    }

    @PutMapping("/me")
    public DoctorChannelView updateMine(@PathVariable UUID clinicId, @RequestBody DoctorChannelUpdate body) {
        return channels.updateMine(currentUserResolver.getCurrentUserId(), clinicId, body);
    }

    @GetMapping("/{staffId}")
    public DoctorChannelView get(@PathVariable UUID clinicId, @PathVariable UUID staffId) {
        return channels.get(currentUserResolver.getCurrentUserId(), clinicId, staffId);
    }

    @PutMapping("/{staffId}")
    public DoctorChannelView update(@PathVariable UUID clinicId, @PathVariable UUID staffId,
                                    @RequestBody DoctorChannelUpdate body) {
        return channels.update(currentUserResolver.getCurrentUserId(), clinicId, staffId, body);
    }
}
