package com.jclinical.agenda.domain.ports.in;

import com.jclinical.agenda.domain.model.WaitingListEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ManageWaitingListUseCase {
    WaitingListEntry addToWaitingList(
            UUID clinicId,
            UUID actingUserId,
            UUID patientId,
            UUID doctorStaffId,
            UUID roomId,
            LocalDate preferredDateFrom,
            LocalDate preferredDateTo,
            String preferredTimeRange,
            String notes
    );

    WaitingListEntry updateStatus(UUID clinicId, UUID entryId, UUID actingUserId, WaitingListEntry.Status status);

    List<WaitingListEntry> listWaitingList(UUID clinicId, UUID actingUserId, boolean waitingOnly);

    void removeFromWaitingList(UUID clinicId, UUID entryId, UUID actingUserId);
}
