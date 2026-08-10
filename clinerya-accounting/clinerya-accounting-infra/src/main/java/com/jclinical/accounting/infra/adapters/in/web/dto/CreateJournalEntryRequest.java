package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.time.LocalDate;
import java.util.List;

public record CreateJournalEntryRequest(
    String description,
    LocalDate entryDate,
    List<CreateJournalLineRequest> lines
) {}
