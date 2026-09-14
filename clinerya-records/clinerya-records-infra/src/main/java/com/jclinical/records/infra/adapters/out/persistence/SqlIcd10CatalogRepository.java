package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.Icd10Code;
import com.jclinical.records.domain.ports.out.Icd10CatalogRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SqlIcd10CatalogRepository implements Icd10CatalogRepositoryPort {

    private final SpringDataIcd10CatalogRepository repository;

    @Override
    public List<Icd10Code> search(String term, int limit) {
        return repository.search(term, PageRequest.of(0, limit)).stream()
                .map(SqlIcd10CatalogRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<Icd10Code> findByCode(String code) {
        return repository.findById(code).map(SqlIcd10CatalogRepository::toDomain);
    }

    private static Icd10Code toDomain(Icd10CodeEntity entity) {
        return Icd10Code.builder()
                .code(entity.getCode())
                .description(entity.getDescription())
                .chapter(entity.getChapter())
                .billable(entity.isBillable())
                .build();
    }
}
