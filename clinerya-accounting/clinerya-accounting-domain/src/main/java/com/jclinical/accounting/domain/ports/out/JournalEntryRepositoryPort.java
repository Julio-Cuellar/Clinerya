package com.jclinical.accounting.domain.ports.out;

import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalQueryResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JournalEntryRepositoryPort {

    JournalEntry save(JournalEntry entry);

    boolean existsBySourceEventId(UUID sourceEventId);

    List<JournalEntry> findByClinicId(UUID clinicId);

    List<JournalEntry> findByClinicIdAndEntryDateBetween(UUID clinicId, LocalDate from, LocalDate to);

    List<JournalEntry> findByClinicIdAndBankAccountId(UUID clinicId, UUID bankAccountId);

    JournalQueryResult queryByClinic(
            UUID clinicId,
            LocalDate from,
            LocalDate to,
            String search,
            String sourceEventType,
            int page,
            int size);
}
