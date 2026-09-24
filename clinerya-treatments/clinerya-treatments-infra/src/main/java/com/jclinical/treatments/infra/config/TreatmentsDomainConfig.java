package com.jclinical.treatments.infra.config;

import com.jclinical.treatments.domain.model.ItemProgressStatus;
import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.model.QuotationItem;
import com.jclinical.treatments.domain.model.QuotationItemMaterial;
import com.jclinical.treatments.domain.model.QuotationStatus;
import com.jclinical.treatments.domain.model.TreatmentCatalogItem;
import com.jclinical.treatments.domain.model.TreatmentCatalogMaterial;
import com.jclinical.treatments.domain.model.Visit;
import com.jclinical.treatments.domain.model.VisitLineItem;
import com.jclinical.treatments.domain.model.VisitMaterialUsage;
import com.jclinical.treatments.domain.ports.out.ClinicSpecialtyPort;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort;
import com.jclinical.treatments.domain.ports.out.PatientValidatorPort;
import com.jclinical.treatments.domain.ports.out.QuotationRepositoryPort;
import com.jclinical.treatments.domain.ports.out.TreatmentCatalogRepositoryPort;
import com.jclinical.treatments.domain.ports.out.VisitRepositoryPort;
import com.jclinical.treatments.domain.service.QuotationService;
import com.jclinical.treatments.domain.service.TreatmentCatalogService;
import com.jclinical.treatments.domain.service.VisitService;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.treatments.infra.adapters.out.persistence.QuotationEntity;
import com.jclinical.treatments.infra.adapters.out.persistence.QuotationItemEntity;
import com.jclinical.treatments.infra.adapters.out.persistence.QuotationItemMapper;
import com.jclinical.treatments.infra.adapters.out.persistence.QuotationItemMaterialEntity;
import com.jclinical.treatments.infra.adapters.out.persistence.QuotationItemMaterialMapper;
import com.jclinical.treatments.infra.adapters.out.persistence.QuotationMapper;
import com.jclinical.treatments.infra.adapters.out.persistence.TreatmentCatalogItemEntity;
import com.jclinical.treatments.infra.adapters.out.persistence.TreatmentCatalogMapper;
import com.jclinical.treatments.infra.adapters.out.persistence.TreatmentCatalogMaterialEntity;
import com.jclinical.treatments.infra.adapters.out.persistence.TreatmentCatalogMaterialMapper;
import com.jclinical.treatments.infra.adapters.out.persistence.VisitEntity;
import com.jclinical.treatments.infra.adapters.out.persistence.VisitLineItemEntity;
import com.jclinical.treatments.infra.adapters.out.persistence.VisitMaterialUsageEntity;
import com.jclinical.treatments.infra.adapters.out.persistence.VisitMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class TreatmentsDomainConfig {

    @Bean
    public TreatmentCatalogService treatmentCatalogService(
            TreatmentCatalogRepositoryPort catalogRepository,
            InventoryMaterialPort inventoryMaterialPort,
            StaffPermissionCheckerPort permissionChecker,
            ClinicSpecialtyPort clinicSpecialtyPort) {
        return new TreatmentCatalogService(
                catalogRepository, inventoryMaterialPort, permissionChecker, clinicSpecialtyPort);
    }

    @Bean
    public QuotationService quotationService(
            QuotationRepositoryPort quotationRepository,
            PatientValidatorPort patientValidator,
            InventoryMaterialPort inventoryMaterialPort,
            PatientAccessAuthorizationPort accessAuthorizationPort,
            ClinicSpecialtyPort clinicSpecialtyPort) {
        return new QuotationService(
                quotationRepository,
                patientValidator,
                inventoryMaterialPort,
                accessAuthorizationPort,
                clinicSpecialtyPort);
    }

    @Bean
    @ConditionalOnMissingBean(TreatmentCatalogMaterialMapper.class)
    public TreatmentCatalogMaterialMapper treatmentCatalogMaterialMapper() {
        return new TreatmentCatalogMaterialMapper() {
            @Override
            public TreatmentCatalogMaterialEntity toEntity(TreatmentCatalogMaterial domain) {
                if (domain == null) {
                    return null;
                }
                return TreatmentCatalogMaterialEntity.builder()
                        .id(domain.getId())
                        .materialId(domain.getMaterialId())
                        .materialName(domain.getMaterialName())
                        .typicalQuantity(domain.getTypicalQuantity())
                        .build();
            }

            @Override
            public TreatmentCatalogMaterial toDomain(TreatmentCatalogMaterialEntity entity) {
                if (entity == null) {
                    return null;
                }
                return TreatmentCatalogMaterial.builder()
                        .id(entity.getId())
                        .materialId(entity.getMaterialId())
                        .materialName(entity.getMaterialName())
                        .typicalQuantity(entity.getTypicalQuantity())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(TreatmentCatalogMapper.class)
    public TreatmentCatalogMapper treatmentCatalogMapper(TreatmentCatalogMaterialMapper materialMapper) {
        return new TreatmentCatalogMapper() {
            @Override
            public TreatmentCatalogItemEntity toEntity(TreatmentCatalogItem domain) {
                if (domain == null) {
                    return null;
                }
                TreatmentCatalogItemEntity entity = TreatmentCatalogItemEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .name(domain.getName())
                        .category(domain.getCategory())
                        .description(domain.getDescription())
                        .defaultPrice(domain.getDefaultPrice())
                        .estimatedDurationMinutes(domain.getEstimatedDurationMinutes())
                        .active(domain.isActive())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .materials(new ArrayList<>())
                        .build();

                List<TreatmentCatalogMaterialEntity> materials = domain.getMaterials() == null
                        ? new ArrayList<>()
                        : domain.getMaterials().stream()
                                .map(materialMapper::toEntity)
                                .peek(material -> material.setCatalogItem(entity))
                                .toList();
                entity.setMaterials(new ArrayList<>(materials));
                return entity;
            }

            @Override
            public TreatmentCatalogItem toDomain(TreatmentCatalogItemEntity entity) {
                if (entity == null) {
                    return null;
                }
                List<TreatmentCatalogMaterial> materials = entity.getMaterials() == null
                        ? new ArrayList<>()
                        : entity.getMaterials().stream()
                                .map(materialMapper::toDomain)
                                .toList();
                return TreatmentCatalogItem.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .name(entity.getName())
                        .category(entity.getCategory())
                        .description(entity.getDescription())
                        .defaultPrice(entity.getDefaultPrice())
                        .estimatedDurationMinutes(entity.getEstimatedDurationMinutes())
                        .active(entity.isActive())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .materials(new ArrayList<>(materials))
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(QuotationItemMaterialMapper.class)
    public QuotationItemMaterialMapper quotationItemMaterialMapper() {
        return new QuotationItemMaterialMapper() {
            @Override
            public QuotationItemMaterialEntity toEntity(QuotationItemMaterial domain) {
                if (domain == null) {
                    return null;
                }
                return QuotationItemMaterialEntity.builder()
                        .id(domain.getId())
                        .materialId(domain.getMaterialId())
                        .materialName(domain.getMaterialName())
                        .estimatedQuantity(domain.getEstimatedQuantity())
                        .unitCostAtQuote(domain.getUnitCostAtQuote())
                        .build();
            }

            @Override
            public QuotationItemMaterial toDomain(QuotationItemMaterialEntity entity) {
                if (entity == null) {
                    return null;
                }
                return QuotationItemMaterial.builder()
                        .id(entity.getId())
                        .materialId(entity.getMaterialId())
                        .materialName(entity.getMaterialName())
                        .estimatedQuantity(entity.getEstimatedQuantity())
                        .unitCostAtQuote(entity.getUnitCostAtQuote())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(QuotationItemMapper.class)
    public QuotationItemMapper quotationItemMapper(QuotationItemMaterialMapper materialMapper) {
        return new QuotationItemMapper() {
            @Override
            public QuotationItemEntity toEntity(QuotationItem domain) {
                if (domain == null) {
                    return null;
                }
                QuotationItemEntity entity = QuotationItemEntity.builder()
                        .id(domain.getId())
                        .catalogItemId(domain.getCatalogItemId())
                        .description(domain.getDescription())
                        .toothNumber(domain.getToothNumber())
                        .laborCharge(domain.getLaborCharge())
                        .discountPercentage(domain.getDiscountPercentage())
                        .progressStatus(domain.getProgressStatus() != null ? domain.getProgressStatus().name() : ItemProgressStatus.PENDING.name())
                        .materials(new ArrayList<>())
                        .build();

                List<QuotationItemMaterialEntity> materials = domain.getMaterials() == null
                        ? new ArrayList<>()
                        : domain.getMaterials().stream()
                                .map(materialMapper::toEntity)
                                .peek(material -> material.setQuotationItem(entity))
                                .toList();
                entity.setMaterials(new ArrayList<>(materials));
                return entity;
            }

            @Override
            public QuotationItem toDomain(QuotationItemEntity entity) {
                if (entity == null) {
                    return null;
                }
                List<QuotationItemMaterial> materials = entity.getMaterials() == null
                        ? new ArrayList<>()
                        : entity.getMaterials().stream()
                                .map(materialMapper::toDomain)
                                .toList();
                return QuotationItem.builder()
                        .id(entity.getId())
                        .catalogItemId(entity.getCatalogItemId())
                        .description(entity.getDescription())
                        .toothNumber(entity.getToothNumber())
                        .laborCharge(entity.getLaborCharge())
                        .discountPercentage(entity.getDiscountPercentage())
                        .progressStatus(entity.getProgressStatus() != null ? ItemProgressStatus.valueOf(entity.getProgressStatus()) : ItemProgressStatus.PENDING)
                        .materials(new ArrayList<>(materials))
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(QuotationMapper.class)
    public QuotationMapper quotationMapper(QuotationItemMapper quotationItemMapper) {
        return new QuotationMapper() {
            @Override
            public QuotationEntity toEntity(Quotation domain) {
                if (domain == null) {
                    return null;
                }
                QuotationEntity entity = QuotationEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .patientId(domain.getPatientId())
                        .createdByUserId(domain.getCreatedByUserId())
                        .quotationDate(domain.getQuotationDate())
                        .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                        .notes(domain.getNotes())
                        .validUntil(domain.getValidUntil())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .items(new ArrayList<>())
                        .build();

                List<QuotationItemEntity> items = domain.getItems() == null
                        ? new ArrayList<>()
                        : domain.getItems().stream()
                                .map(quotationItemMapper::toEntity)
                                .peek(item -> item.setQuotation(entity))
                                .toList();
                entity.setItems(new ArrayList<>(items));
                return entity;
            }

            @Override
            public Quotation toDomain(QuotationEntity entity) {
                if (entity == null) {
                    return null;
                }
                List<QuotationItem> items = entity.getItems() == null
                        ? new ArrayList<>()
                        : entity.getItems().stream()
                                .map(quotationItemMapper::toDomain)
                                .toList();
                return Quotation.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .patientId(entity.getPatientId())
                        .createdByUserId(entity.getCreatedByUserId())
                        .quotationDate(entity.getQuotationDate())
                        .status(entity.getStatus() != null ? QuotationStatus.valueOf(entity.getStatus()) : null)
                        .notes(entity.getNotes())
                        .validUntil(entity.getValidUntil())
                        .items(new ArrayList<>(items))
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };
    }

    @Bean
    public VisitService visitService(
            VisitRepositoryPort visitRepository,
            QuotationRepositoryPort quotationRepository,
            PatientValidatorPort patientValidator,
            InventoryMaterialPort inventoryMaterialPort,
            com.jclinical.core.events.DomainEventPublisherPort eventPublisher,
            PatientAccessAuthorizationPort accessAuthorizationPort) {
        return new VisitService(visitRepository, quotationRepository, patientValidator, inventoryMaterialPort,
                eventPublisher, accessAuthorizationPort);
    }

    @Bean
    @ConditionalOnMissingBean(VisitMapper.class)
    public VisitMapper visitMapper() {
        return new VisitMapper() {
            @Override
            public VisitEntity toEntity(Visit domain) {
                if (domain == null) {
                    return null;
                }
                VisitEntity visitEntity = VisitEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .patientId(domain.getPatientId())
                        .quotationId(domain.getQuotationId())
                        .visitDate(domain.getVisitDate())
                        .doctorId(domain.getDoctorId())
                        .notes(domain.getNotes())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .items(new ArrayList<>())
                        .build();

                List<VisitLineItemEntity> items = domain.getItems() == null
                        ? new ArrayList<>()
                        : domain.getItems().stream()
                                .map(item -> toEntity(item, visitEntity))
                                .toList();
                visitEntity.setItems(new ArrayList<>(items));
                return visitEntity;
            }

            private VisitLineItemEntity toEntity(VisitLineItem domain, VisitEntity visitEntity) {
                VisitLineItemEntity itemEntity = VisitLineItemEntity.builder()
                        .id(domain.getId())
                        .visit(visitEntity)
                        .quotationItemId(domain.getQuotationItemId())
                        .materials(new ArrayList<>())
                        .build();

                List<VisitMaterialUsageEntity> materials = domain.getMaterialsUsed() == null
                        ? new ArrayList<>()
                        : domain.getMaterialsUsed().stream()
                                .map(usage -> toEntity(usage, itemEntity))
                                .toList();
                itemEntity.setMaterials(new ArrayList<>(materials));
                return itemEntity;
            }

            private VisitMaterialUsageEntity toEntity(VisitMaterialUsage domain, VisitLineItemEntity itemEntity) {
                return VisitMaterialUsageEntity.builder()
                        .id(domain.getId())
                        .visitLineItem(itemEntity)
                        .materialId(domain.getMaterialId())
                        .materialName(domain.getMaterialName())
                        .actualQuantity(domain.getActualQuantity())
                        .build();
            }

            @Override
            public Visit toDomain(VisitEntity entity) {
                if (entity == null) {
                    return null;
                }
                List<VisitLineItem> items = entity.getItems() == null
                        ? new ArrayList<>()
                        : entity.getItems().stream()
                                .map(this::toDomain)
                                .toList();
                return new Visit(
                        entity.getId(),
                        entity.getClinicId(),
                        entity.getPatientId(),
                        entity.getQuotationId(),
                        entity.getVisitDate(),
                        entity.getDoctorId(),
                        entity.getNotes(),
                        new ArrayList<>(items),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()
                );
            }

            private VisitLineItem toDomain(VisitLineItemEntity entity) {
                List<VisitMaterialUsage> materials = entity.getMaterials() == null
                        ? new ArrayList<>()
                        : entity.getMaterials().stream()
                                .map(this::toDomain)
                                .toList();
                return new VisitLineItem(
                        entity.getId(),
                        entity.getQuotationItemId(),
                        new ArrayList<>(materials)
                );
            }

            private VisitMaterialUsage toDomain(VisitMaterialUsageEntity entity) {
                return new VisitMaterialUsage(
                        entity.getId(),
                        entity.getMaterialId(),
                        entity.getMaterialName(),
                        entity.getActualQuantity()
                );
            }
        };
    }
}
