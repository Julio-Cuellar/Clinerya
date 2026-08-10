package com.jclinical.accounting.infra.adapters.out.persistence;

import com.jclinical.accounting.domain.model.JournalEntry;

public interface JournalEntryMapper {

    JournalEntryEntity toEntity(JournalEntry domain);

    JournalEntry toDomain(JournalEntryEntity entity);
}
