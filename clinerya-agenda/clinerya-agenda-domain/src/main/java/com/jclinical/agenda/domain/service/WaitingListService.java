package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.WaitingListEntry;
import com.jclinical.agenda.domain.ports.in.ManageWaitingListUseCase;
import com.jclinical.agenda.domain.ports.out.PatientValidatorPort;
import com.jclinical.agenda.domain.ports.out.WaitingListRepositoryPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class WaitingListService implements ManageWaitingListUseCase {

    private final WaitingListRepositoryPort waitingListRepository;
    private final PatientValidatorPort patientValidator;

    public WaitingListService(WaitingListRepositoryPort waitingListRepository, PatientValidatorPort patientValidator) {
        this.waitingListRepository = waitingListRepository;
        this.patientValidator = patientValidator;
    }

    @Override
    public WaitingListEntry addToWaitingList(
            UUID clinicId,
            UUID patientId,
            UUID doctorStaffId,
            UUID roomId,
            LocalDate preferredDateFrom,
            LocalDate preferredDateTo,
            String preferredTimeRange,
            String notes) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clínica.");
        }

        WaitingListEntry entry = WaitingListEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .patientId(patientId)
                .doctorStaffId(doctorStaffId)
                .roomId(roomId)
                .preferredDateFrom(preferredDateFrom)
                .preferredDateTo(preferredDateTo)
                .preferredTimeRange(preferredTimeRange != null ? preferredTimeRange : "ANY")
                .notes(notes)
                .status(WaitingListEntry.Status.WAITING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return waitingListRepository.save(entry);
    }

    @Override
    public WaitingListEntry updateStatus(UUID clinicId, UUID entryId, WaitingListEntry.Status status) {
        WaitingListEntry entry = waitingListRepository.findByIdAndClinicId(entryId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La entrada en lista de espera no existe."));
        entry.setStatus(status);
        entry.setUpdatedAt(LocalDateTime.now());
        return waitingListRepository.save(entry);
    }

    @Override
    public List<WaitingListEntry> listWaitingList(UUID clinicId, boolean waitingOnly) {
        return waitingOnly
                ? waitingListRepository.findByClinicIdAndStatus(clinicId, WaitingListEntry.Status.WAITING)
                : waitingListRepository.findByClinicId(clinicId);
    }

    @Override
    public void removeFromWaitingList(UUID clinicId, UUID entryId) {
        WaitingListEntry entry = waitingListRepository.findByIdAndClinicId(entryId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La entrada en lista de espera no existe."));
        waitingListRepository.delete(entry);
    }
}
