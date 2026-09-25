package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.DoctorChannel;
import com.jclinical.automation.domain.ports.in.ManageDoctorChannelUseCase;
import com.jclinical.automation.domain.ports.out.DoctorChannelRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorIdentityPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Celular donde el medico recibe avisos (D8). El propio medico lo registra; quien administra las
 * integraciones puede hacerlo por cualquier medico de la clinica. Sin consentimiento no se activa y
 * retirarlo apaga los avisos. El numero se guarda como WhatsApp identifica al remitente, para
 * reconocer al medico si escribe.
 */
public class DoctorChannelService implements ManageDoctorChannelUseCase {

    static final int MIN_DIGITS = 10;
    static final int MAX_DIGITS = 15;
    /** Mexico: WhatsApp reporta los celulares como 52 + 1 + 10 digitos. */
    private static final int NATIONAL_DIGITS = 10;
    private static final String MEXICO = "52";
    private static final String MEXICO_MOBILE = "521";
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("[+\\d\\s().-]+");

    private final DoctorChannelRepositoryPort channels;
    private final DoctorIdentityPort identity;
    private final DoctorDirectoryPort doctors;
    private final StaffPermissionCheckerPort permissions;
    private final Clock clock;

    public DoctorChannelService(DoctorChannelRepositoryPort channels, DoctorIdentityPort identity,
                                DoctorDirectoryPort doctors, StaffPermissionCheckerPort permissions, Clock clock) {
        this.channels = channels;
        this.identity = identity;
        this.doctors = doctors;
        this.permissions = permissions;
        this.clock = clock;
    }

    @Override
    public DoctorChannelView getMine(UUID actingUserId, UUID clinicId) {
        UUID staffId = ownStaffId(actingUserId, clinicId);
        return view(staffId, channels.find(clinicId, staffId));
    }

    @Override
    public DoctorChannelView updateMine(UUID actingUserId, UUID clinicId, DoctorChannelUpdate update) {
        return save(actingUserId, clinicId, ownStaffId(actingUserId, clinicId), update);
    }

    @Override
    public DoctorChannelView get(UUID actingUserId, UUID clinicId, UUID staffId) {
        requireSelfOrIntegrationManager(actingUserId, clinicId, staffId);
        return view(staffId, channels.find(clinicId, staffId));
    }

    @Override
    public DoctorChannelView update(UUID actingUserId, UUID clinicId, UUID staffId, DoctorChannelUpdate update) {
        requireSelfOrIntegrationManager(actingUserId, clinicId, staffId);
        if (doctors.listDoctors(clinicId).stream().noneMatch(doctor -> doctor.staffId().equals(staffId))) {
            throw new IllegalArgumentException("Ese miembro del personal no atiende pacientes.");
        }
        return save(actingUserId, clinicId, staffId, update);
    }

    private DoctorChannelView save(UUID actingUserId, UUID clinicId, UUID staffId, DoctorChannelUpdate update) {
        if (update == null) {
            throw new IllegalArgumentException("Indica el celular del médico.");
        }
        String phone = normalize(update.phone());
        if (update.active() && !update.consent()) {
            throw new IllegalArgumentException("Para recibir avisos por WhatsApp el médico debe aceptar recibirlos.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime consentAt = !update.consent() ? null : channels.find(clinicId, staffId)
                .map(DoctorChannel::consentAt)
                .filter(Objects::nonNull)
                .orElse(now);
        DoctorChannel saved = channels.save(new DoctorChannel(clinicId, staffId, phone, update.active(), consentAt,
                actingUserId, now));
        return view(staffId, Optional.of(saved));
    }

    /** 10 digitos se toman como celular de Mexico; con lada internacional se respeta tal cual. */
    static String normalize(String raw) {
        if (raw == null || !ALLOWED_CHARACTERS.matcher(raw).matches()) {
            throw new IllegalArgumentException("Escribe el celular solo con números, por ejemplo 55 1234 5678.");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == NATIONAL_DIGITS) {
            return MEXICO_MOBILE + digits;
        }
        if (digits.length() == MEXICO.length() + NATIONAL_DIGITS && digits.startsWith(MEXICO)) {
            return MEXICO_MOBILE + digits.substring(MEXICO.length());
        }
        if (digits.length() < MIN_DIGITS || digits.length() > MAX_DIGITS) {
            throw new IllegalArgumentException("El celular debe tener entre " + MIN_DIGITS + " y " + MAX_DIGITS + " dígitos.");
        }
        return digits;
    }

    private UUID ownStaffId(UUID actingUserId, UUID clinicId) {
        return staffIdOf(actingUserId, clinicId).orElseThrow(() -> new ClinicAccessDeniedException(
                "Solo el personal que atiende pacientes recibe avisos de solicitudes por WhatsApp."));
    }

    private void requireSelfOrIntegrationManager(UUID actingUserId, UUID clinicId, UUID staffId) {
        boolean self = staffIdOf(actingUserId, clinicId).map(staffId::equals).orElse(false);
        boolean manager = actingUserId != null
                && permissions.hasPermission(clinicId, actingUserId, StaffPermission.MANAGE_INTEGRATIONS);
        if (!self && !manager) {
            throw new ClinicAccessDeniedException(
                    "Solo el propio médico o quien administra las integraciones puede ver o cambiar este número.");
        }
    }

    private Optional<UUID> staffIdOf(UUID actingUserId, UUID clinicId) {
        return actingUserId == null ? Optional.empty() : identity.doctorStaffIdOfUser(clinicId, actingUserId);
    }

    private static DoctorChannelView view(UUID staffId, Optional<DoctorChannel> channel) {
        return channel
                .map(saved -> new DoctorChannelView(staffId, saved.phone(), saved.active(), saved.consentAt(), saved.updatedAt()))
                .orElseGet(() -> new DoctorChannelView(staffId, null, false, null, null));
    }
}
