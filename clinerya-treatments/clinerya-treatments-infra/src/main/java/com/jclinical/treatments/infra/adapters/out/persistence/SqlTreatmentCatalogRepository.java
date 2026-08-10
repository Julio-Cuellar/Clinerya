package com.jclinical.treatments.infra.adapters.out.persistence;

import com.jclinical.treatments.domain.model.TreatmentCatalogItem;
import com.jclinical.treatments.domain.ports.out.TreatmentCatalogRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlTreatmentCatalogRepository implements TreatmentCatalogRepositoryPort {

    private final SpringDataTreatmentCatalogRepository springRepository;
    private final TreatmentCatalogMapper mapper;

    @Override
    public TreatmentCatalogItem save(TreatmentCatalogItem item) {
        TreatmentCatalogItemEntity entity = mapper.toEntity(item);
        TreatmentCatalogItemEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<TreatmentCatalogItem> findByIdAndClinicId(UUID itemId, UUID clinicId) {
        return springRepository.findByIdAndClinicId(itemId, clinicId).map(mapper::toDomain);
    }

    @Override
    public List<TreatmentCatalogItem> findByClinicId(UUID clinicId, boolean includeInactive) {
        List<TreatmentCatalogItemEntity> entities = includeInactive
                ? springRepository.findByClinicId(clinicId)
                : springRepository.findByClinicIdAndActiveTrue(clinicId);
        return entities.stream().map(mapper::toDomain).toList();
    }
}
