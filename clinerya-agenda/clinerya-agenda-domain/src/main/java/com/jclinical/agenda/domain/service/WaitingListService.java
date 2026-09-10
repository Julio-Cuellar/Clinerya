package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.WaitingListEntry;
import com.jclinical.agenda.domain.ports.in.ManageWaitingListUseCase;
import com.jclinical.agenda.domain.ports.out.PatientValidatorPort;
import com.jclinical.agenda.domain.ports.out.WaitingListRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class WaitingListService implements ManageWaitingListUseCase {

    private final WaitingListRepositoryPort waitingListRepository;
    private final PatientValidatorPort patientValidator;
    private final StaffPermissionCheckerPort permissionChecker;

    public WaitingListService(
            WaitingListRepositoryPort waitingListRepository,
            PatientValidatorPort patientValidator,
            StaffPermissionCheckerPort permissionChecker) {
        this.waitingListRepository = waitingListRepository;
        this.patientValidator = patientValidator;
        this.permissionChecker = permissionChecker;
    }

    private void authorize(UUID actingUserId, UUID clinicId) {
        if (actingUserId == null
                || !permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.MANAGE_WAITING_LIST)) {
            throw new ClinicAccessDeniedException("No tienes permisos para esta operación de agenda.");
        }
    }

    @Override
    public WaitingListEntry addToWaitingList(
            UUID clinicId,
            UUID actingUserId,
            UUID patientId,
            UUID doctorStaffId,
            UUID roomId,
            LocalDate preferredDateFrom,
            LocalDate preferredDateTo,
            String preferredTimeRange,
            String notes) {
        authorize(actingUserId, clinicId);
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
    public WaitingListEntry updateStatus(UUID clinicId, UUID entryId, UUID actingUserId, WaitingListEntry.Status status) {
        authorize(actingUserId, clinicId);
        WaitingListEntry entry = waitingListRepository.findByIdAndClinicId(entryId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La entrada en lista de espera no existe."));
        entry.setStatus(status);
        entry.setUpdatedAt(LocalDateTime.now());
        return waitingListRepository.save(entry);
    }

    @Override
    public List<WaitingListEntry> listWaitingList(UUID clinicId, UUID actingUserId, boolean waitingOnly) {
        authorize(actingUserId, clinicId);
        return waitingOnly
                ? waitingListRepository.findByClinicIdAndStatus(clinicId, WaitingListEntry.Status.WAITING)
                : waitingListRepository.findByClinicId(clinicId);
    }

    @Override
    public void removeFromWaitingList(UUID clinicId, UUID entryId, UUID actingUserId) {
        authorize(actingUserId, clinicId);
        WaitingListEntry entry = waitingListRepository.findByIdAndClinicId(entryId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La entrada en lista de espera no existe."));
        waitingListRepository.delete(entry);
    }
}
