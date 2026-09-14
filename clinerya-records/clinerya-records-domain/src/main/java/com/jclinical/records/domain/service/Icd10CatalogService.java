package com.jclinical.records.domain.service;

import com.jclinical.records.domain.model.Icd10Code;
import com.jclinical.records.domain.ports.in.ManageIcd10CatalogUseCase;
import com.jclinical.records.domain.ports.out.Icd10CatalogRepositoryPort;

import java.util.List;

public class Icd10CatalogService implements ManageIcd10CatalogUseCase {

    private static final int MIN_TERM_LENGTH = 2;
    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_LIMIT = 20;

    private final Icd10CatalogRepositoryPort repository;

    public Icd10CatalogService(Icd10CatalogRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public List<Icd10Code> search(String term, int limit) {
        if (term == null || term.trim().length() < MIN_TERM_LENGTH) {
            return List.of();
        }
        int effectiveLimit = limit <= 0 || limit > MAX_LIMIT ? DEFAULT_LIMIT : limit;
        return repository.search(term.trim(), effectiveLimit);
    }
}
