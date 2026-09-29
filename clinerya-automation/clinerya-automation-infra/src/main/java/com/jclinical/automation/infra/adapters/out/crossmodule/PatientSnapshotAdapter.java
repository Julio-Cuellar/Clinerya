package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.VisitSummary;
import com.jclinical.automation.domain.ports.out.PatientSnapshotPort;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Pacientes con ese celular y su resumen de visitas, por las rutas publicas de pacientes y agenda. El
 * celular se compara por sus ultimos 10 digitos, igual que el directorio de pacientes.
 */
public class PatientSnapshotAdapter implements PatientSnapshotPort {

    private final GetPatientUseCase patients;
    private final OnlineBookingUseCase agenda;

    public PatientSnapshotAdapter(GetPatientUseCase patients, OnlineBookingUseCase agenda) {
        this.patients = patients;
        this.agenda = agenda;
    }

    @Override
    public List<PatientSnapshot> findByPhone(UUID clinicId, String phone) {
        String wanted = PatientDirectoryAdapter.nationalNumber(phone);
        if (wanted == null) {
            return List.of();
        }
        return patients.getPatientsByClinic(clinicId).stream()
                .filter(patient -> wanted.equals(PatientDirectoryAdapter.nationalNumber(patient.getPhone())))
                .map(patient -> snapshot(clinicId, patient))
                .toList();
    }

    private PatientSnapshot snapshot(UUID clinicId, Patient patient) {
        VisitSummary visits = agenda.visitSummary(clinicId, patient.getId());
        boolean consent = patient.getContactConsent() != null && patient.getContactConsent().allowsProactiveContact();
        return new PatientSnapshot(patient.getId(), fullName(patient), patient.getDateOfBirth(), patient.getCreatedAt(),
                consent, visits.nextStart(), visits.nextDoctorStaffId(), visits.lastAttendedStart(),
                visits.lastAttendedDoctorStaffId(), visits.attended(), visits.cancelled(), visits.noShows());
    }

    private static String fullName(Patient patient) {
        return Stream.of(patient.getFirstName(), patient.getLastNamePaterno(), patient.getLastNameMaterno())
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(" "));
    }
}
