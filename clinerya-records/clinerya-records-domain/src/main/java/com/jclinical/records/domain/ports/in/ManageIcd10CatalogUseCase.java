package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.Icd10Code;

import java.util.List;

public interface ManageIcd10CatalogUseCase {

    List<Icd10Code> search(String term, int limit);
}
