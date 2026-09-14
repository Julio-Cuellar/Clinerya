package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.Icd10Code;

import java.util.List;
import java.util.Optional;

public interface Icd10CatalogRepositoryPort {

    List<Icd10Code> search(String term, int limit);

    Optional<Icd10Code> findByCode(String code);
}
