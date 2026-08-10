package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.MedicalHistoryVersion;
import com.jclinical.records.domain.ports.out.MedicalHistoryVersionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlMedicalHistoryVersionRepository implements MedicalHistoryVersionRepositoryPort {

    private final SpringDataMedicalHistoryVersionRepository springRepository;
    private final MedicalHistoryVersionMapper mapper;

    @Override
    public MedicalHistoryVersion save(MedicalHistoryVersion version) {
        MedicalHistoryVersionEntity entity = mapper.toEntity(version);
        MedicalHistoryVersionEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<MedicalHistoryVersion> findByMedicalHistoryIdOrderByVersionDesc(UUID medicalHistoryId) {
        return springRepository.findByMedicalHistoryIdOrderByVersionDesc(medicalHistoryId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<MedicalHistoryVersion> findByMedicalHistoryIdAndVersion(UUID medicalHistoryId, int version) {
        return springRepository.findByMedicalHistoryIdAndVersion(medicalHistoryId, version)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<MedicalHistoryVersion> findTopByMedicalHistoryIdOrderByVersionDesc(UUID medicalHistoryId) {
        return springRepository.findTopByMedicalHistoryIdOrderByVersionDesc(medicalHistoryId)
                .map(mapper::toDomain);
    }
}
