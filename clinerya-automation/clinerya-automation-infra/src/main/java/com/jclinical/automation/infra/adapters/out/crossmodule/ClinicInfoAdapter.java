package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.ports.in.ManageClinicScheduleUseCase;
import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.model.ClinicInfo.OpeningHours;
import com.jclinical.automation.domain.ports.out.ClinicInfoPort;
import com.jclinical.clinics.domain.ports.in.GetClinicPublicProfileUseCase;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que el asistente puede decir de la clinica: su perfil publico (modulo de clinicas) y su horario
 * semanal (agenda), ambos por su API publica.
 */
public class ClinicInfoAdapter implements ClinicInfoPort {

    private final GetClinicPublicProfileUseCase profiles;
    private final ManageClinicScheduleUseCase schedules;

    public ClinicInfoAdapter(GetClinicPublicProfileUseCase profiles, ManageClinicScheduleUseCase schedules) {
        this.profiles = profiles;
        this.schedules = schedules;
    }

    @Override
    public Optional<ClinicInfo> find(UUID clinicId) {
        return profiles.getPublicProfile(clinicId).map(profile -> new ClinicInfo(profile.name(), profile.address(),
                profile.phone(), profile.email(), profile.privacyNoticeUrl(), hoursOf(clinicId)));
    }

    private List<OpeningHours> hoursOf(UUID clinicId) {
        return schedules.getSchedule(clinicId).stream()
                .sorted(Comparator.comparing(ClinicSchedule::getDayOfWeek))
                .map(day -> new OpeningHours(day.getDayOfWeek(), day.isOpen(), day.getStartTime(), day.getEndTime()))
                .toList();
    }
}
