package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChatContact;
import com.jclinical.automation.domain.model.ChatContact.ContactPatient;
import com.jclinical.automation.domain.model.ChatContact.Signal;
import com.jclinical.automation.domain.ports.in.ViewChatContactUseCase;
import com.jclinical.automation.domain.ports.out.ChatContactsPort;
import com.jclinical.automation.domain.ports.out.ChatContactsPort.ChatFacts;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;
import com.jclinical.automation.domain.ports.out.PatientSnapshotPort;
import com.jclinical.automation.domain.ports.out.PatientSnapshotPort.PatientSnapshot;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ficha breve del contacto de un chat, para reconocer a quien escribe o sospechar de spam. Las senales
 * solo orientan: no bloquean ni cambian lo que hace el agente.
 */
public class ChatContactService implements ViewChatContactUseCase {

    /** Lada de Mexico en formato internacional: las clinicas atienden en Mexico. */
    static final String MEXICO_PREFIX = "52";

    private final ChatContactsPort contacts;
    private final PatientSnapshotPort patients;
    private final DoctorDirectoryPort doctors;
    private final StaffPermissionCheckerPort permissions;
    private final Clock clock;

    public ChatContactService(ChatContactsPort contacts, PatientSnapshotPort patients, DoctorDirectoryPort doctors,
                              StaffPermissionCheckerPort permissions, Clock clock) {
        this.contacts = contacts;
        this.patients = patients;
        this.doctors = doctors;
        this.permissions = permissions;
        this.clock = clock;
    }

    @Override
    public ChatContact contact(UUID actingUserId, UUID clinicId, String phone) {
        if (actingUserId == null || !permissions.hasPermission(clinicId, actingUserId, StaffPermission.VIEW_PATIENTS)) {
            throw new ClinicAccessDeniedException("No tienes permiso para ver los contactos de WhatsApp de esta clínica.");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Indica el chat.");
        }
        Optional<ChatFacts> facts = contacts.facts(clinicId, phone);
        List<PatientSnapshot> snapshots = patients.findByPhone(clinicId, phone);
        Map<UUID, String> doctorNames = snapshots.isEmpty() ? Map.of() : doctors.listDoctors(clinicId).stream()
                .collect(Collectors.toMap(DoctorContact::staffId, DoctorContact::displayName, (first, second) -> first));
        LocalDate today = LocalDate.now(clock);
        List<ContactPatient> contactPatients = snapshots.stream()
                .map(snapshot -> toContactPatient(snapshot, doctorNames, today))
                .toList();
        return new ChatContact(phone, facts.map(ChatFacts::profileName).orElse(null),
                facts.map(ChatFacts::firstMessageAt).orElse(null), facts.map(ChatFacts::messageCount).orElse(0),
                contactPatients, signals(phone, facts, snapshots));
    }

    private static ContactPatient toContactPatient(PatientSnapshot snapshot, Map<UUID, String> doctorNames, LocalDate today) {
        Integer age = snapshot.dateOfBirth() == null ? null : Period.between(snapshot.dateOfBirth(), today).getYears();
        return new ContactPatient(snapshot.patientId(), snapshot.fullName(), age, snapshot.registeredAt(),
                snapshot.whatsappConsent(), snapshot.nextStart(), doctorNames.get(snapshot.nextDoctorStaffId()),
                snapshot.lastAttendedStart(), doctorNames.get(snapshot.lastAttendedDoctorStaffId()), snapshot.attended(),
                snapshot.cancelled(), snapshot.noShows());
    }

    private static List<Signal> signals(String phone, Optional<ChatFacts> facts, List<PatientSnapshot> snapshots) {
        List<Signal> signals = new ArrayList<>();
        if (!phone.replaceAll("[^0-9]", "").startsWith(MEXICO_PREFIX)) {
            signals.add(Signal.FOREIGN_NUMBER);
        }
        if (facts.map(ChatFacts::firstInboundText).filter(ChatContactService::hasLink).isPresent()) {
            signals.add(Signal.FIRST_MESSAGE_HAS_LINK);
        }
        boolean hadAppointment = snapshots.stream().anyMatch(snapshot -> snapshot.nextStart() != null
                || snapshot.attended() + snapshot.cancelled() + snapshot.noShows() > 0);
        if (!hadAppointment) {
            signals.add(Signal.NEVER_HAD_APPOINTMENT);
        }
        return signals;
    }

    private static boolean hasLink(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("http://") || lower.contains("https://") || lower.contains("www.");
    }
}
