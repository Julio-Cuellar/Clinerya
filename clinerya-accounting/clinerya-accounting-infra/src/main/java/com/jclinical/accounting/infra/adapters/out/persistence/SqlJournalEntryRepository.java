package com.jclinical.accounting.infra.adapters.out.persistence;

import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalQueryResult;
import com.jclinical.accounting.domain.ports.out.JournalEntryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlJournalEntryRepository implements JournalEntryRepositoryPort {

    private final SpringDataJournalEntryRepository springRepository;
    private final JournalEntryMapper mapper;

    @Override
    public JournalEntry save(JournalEntry entry) {
        JournalEntryEntity entity = mapper.toEntity(entry);
        JournalEntryEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsBySourceEventId(UUID sourceEventId) {
        return springRepository.existsBySourceEventId(sourceEventId);
    }

    @Override
    public List<JournalEntry> findByClinicId(UUID clinicId) {
        return springRepository.findByClinicIdOrderByCreatedAtDesc(clinicId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<JournalEntry> findByClinicIdAndEntryDateBetween(UUID clinicId, LocalDate from, LocalDate to) {
        return springRepository.findByClinicIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(clinicId, from, to)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<JournalEntry> findByClinicIdAndBankAccountId(UUID clinicId, UUID bankAccountId) {
        return springRepository.findByClinicIdAndBankAccountId(clinicId, bankAccountId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public JournalQueryResult queryByClinic(
            UUID clinicId,
            LocalDate from,
            LocalDate to,
            String search,
            String sourceEventType,
            int page,
            int size) {
        var resultPage = springRepository.search(
                clinicId, from, to, search, sourceEventType, PageRequest.of(page, size));
        List<Object[]> totalRows = springRepository.totals(clinicId, from, to, search, sourceEventType);
        Object[] totals = totalRows == null || totalRows.isEmpty() ? null : totalRows.get(0);
        BigDecimal totalDebit = totals != null && totals.length > 0 && totals[0] != null
                ? toBigDecimal(totals[0]) : BigDecimal.ZERO;
        BigDecimal totalCredit = totals != null && totals.length > 1 && totals[1] != null
                ? toBigDecimal(totals[1]) : BigDecimal.ZERO;
        return new JournalQueryResult(
                resultPage.getContent().stream().map(mapper::toDomain).toList(),
                resultPage.getTotalElements(), totalDebit, totalCredit,
                resultPage.getNumber(), resultPage.getSize(), resultPage.getTotalPages());
    }

    private BigDecimal toBigDecimal(Object value) {
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }
}
