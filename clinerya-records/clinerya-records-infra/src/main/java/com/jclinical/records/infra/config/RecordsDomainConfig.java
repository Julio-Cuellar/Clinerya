package com.jclinical.records.infra.config;

import java.util.List;
import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.DocumentSignature;
import com.jclinical.records.domain.model.DocumentSignatureStatus;
import com.jclinical.records.domain.model.MedicalHistory;
import com.jclinical.records.domain.model.MedicalHistoryTemplate;
import com.jclinical.records.domain.model.MedicalHistoryVersion;
import com.jclinical.records.domain.model.RecordAccessLog;
import com.jclinical.records.domain.model.PrivacyConsent;
import com.jclinical.records.domain.model.NoteStatus;
import com.jclinical.records.domain.model.SignatureDocumentType;
import com.jclinical.records.domain.model.SignerType;
import com.jclinical.records.domain.model.VitalSigns;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.records.domain.ports.out.DocumentSignatureRepositoryPort;
import com.jclinical.records.domain.ports.out.MedicalHistoryRepositoryPort;
import com.jclinical.records.domain.ports.out.MedicalHistoryTemplateRepositoryPort;
import com.jclinical.records.domain.ports.out.MedicalHistoryVersionRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;
import com.jclinical.records.domain.ports.out.TemporaryRecordShareRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientLookupPort;
import com.jclinical.records.domain.ports.out.ClinicLookupPort;
import com.jclinical.records.domain.ports.out.RecordAccessLogRepositoryPort;
import com.jclinical.records.domain.ports.out.RecordAccessLogOutboxPort;
import com.jclinical.records.domain.ports.out.PrivacyConsentRepositoryPort;
import com.jclinical.records.domain.service.ClinicalNoteService;
import com.jclinical.records.domain.service.HistoryTemplateService;
import com.jclinical.records.domain.service.MedicalHistoryService;
import com.jclinical.records.domain.service.RecordAccessLogService;
import com.jclinical.records.domain.service.PrivacyConsentService;
import com.jclinical.records.domain.service.TemporaryRecordShareService;
import com.jclinical.records.infra.adapters.out.persistence.ClinicalNoteEntity;
import com.jclinical.records.infra.adapters.out.persistence.ClinicalNoteMapper;
import com.jclinical.records.infra.adapters.out.persistence.DocumentSignatureEntity;
import com.jclinical.records.infra.adapters.out.persistence.DocumentSignatureMapper;
import com.jclinical.records.infra.adapters.out.persistence.MedicalHistoryEntity;
import com.jclinical.records.infra.adapters.out.persistence.MedicalHistoryMapper;
import com.jclinical.records.infra.adapters.out.persistence.MedicalHistoryTemplateEntity;
import com.jclinical.records.infra.adapters.out.persistence.MedicalHistoryTemplateMapper;
import com.jclinical.records.infra.adapters.out.persistence.MedicalHistoryVersionEntity;
import com.jclinical.records.infra.adapters.out.persistence.MedicalHistoryVersionMapper;
import com.jclinical.records.infra.adapters.out.persistence.RecordAccessLogEntity;
import com.jclinical.records.domain.model.Prescription;
import com.jclinical.records.domain.model.PrescriptionItem;
import com.jclinical.records.domain.model.PrescriptionStatus;
import com.jclinical.records.domain.ports.out.PrescriptionRepositoryPort;
import com.jclinical.records.domain.service.PrescriptionService;
import com.jclinical.records.infra.adapters.out.persistence.PrescriptionEntity;
import com.jclinical.records.infra.adapters.out.persistence.PrescriptionItemEntity;
import com.jclinical.records.infra.adapters.out.persistence.PrescriptionMapper;
import com.jclinical.records.infra.adapters.out.persistence.RecordAccessLogMapper;
import com.jclinical.records.infra.adapters.out.persistence.PrivacyConsentEntity;
import com.jclinical.records.infra.adapters.out.persistence.PrivacyConsentMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

@Configuration
public class RecordsDomainConfig {

    @Bean
    public PrescriptionService prescriptionService(
            PrescriptionRepositoryPort prescriptionRepositoryPort,
            PatientValidatorPort patientValidator,
            PatientAccessAuthorizationPort accessAuthorizationPort) {
        return new PrescriptionService(prescriptionRepositoryPort, patientValidator, accessAuthorizationPort);
    }

    @Bean
    @ConditionalOnMissingBean(PrescriptionMapper.class)
    public PrescriptionMapper prescriptionMapper() {
        return new PrescriptionMapper() {
            @Override
            public PrescriptionEntity toEntity(Prescription domain) {
                if (domain == null) return null;
                PrescriptionEntity entity = PrescriptionEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .patientId(domain.getPatientId())
                        .doctorId(domain.getDoctorId())
                        .appointmentId(domain.getAppointmentId())
                        .notes(domain.getNotes())
                        .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();

                if (domain.getItems() != null) {
                    entity.setItems(domain.getItems().stream()
                            .map(item -> PrescriptionItemEntity.builder()
                                    .id(item.getId())
                                    .prescription(entity)
                                    .medicationName(item.getMedicationName())
                                    .dosage(item.getDosage())
                                    .frequency(item.getFrequency())
                                    .duration(item.getDuration())
                                    .instructions(item.getInstructions())
                                    .build())
                            .collect(Collectors.toList()));
                }
                return entity;
            }

            @Override
            public Prescription toDomain(PrescriptionEntity entity) {
                if (entity == null) return null;
                return Prescription.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .patientId(entity.getPatientId())
                        .doctorId(entity.getDoctorId())
                        .appointmentId(entity.getAppointmentId())
                        .notes(entity.getNotes())
                        .status(entity.getStatus() != null ? PrescriptionStatus.valueOf(entity.getStatus()) : null)
                        .items(entity.getItems() == null ? List.of() :
                                entity.getItems().stream()
                                        .map(item -> PrescriptionItem.builder()
                                                .id(item.getId())
                                                .prescriptionId(entity.getId())
                                                .medicationName(item.getMedicationName())
                                                .dosage(item.getDosage())
                                                .frequency(item.getFrequency())
                                                .duration(item.getDuration())
                                                .instructions(item.getInstructions())
                                                .build())
                                        .collect(Collectors.toList()))
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };
    }

    @Bean
    public HistoryTemplateService historyTemplateService(
            MedicalHistoryTemplateRepositoryPort templateRepository) {
        return new HistoryTemplateService(templateRepository);
    }

    @Bean
    public MedicalHistoryService medicalHistoryService(
            MedicalHistoryRepositoryPort historyRepository,
            MedicalHistoryTemplateRepositoryPort templateRepository,
            PatientValidatorPort patientValidator,
            PatientAccessAuthorizationPort accessAuthorizationPort,
            MedicalHistoryVersionRepositoryPort versionRepository) {
        return new MedicalHistoryService(historyRepository, templateRepository, patientValidator, accessAuthorizationPort, versionRepository);
    }

    @Bean
    public ClinicalNoteService clinicalNoteService(
            ClinicalNoteRepositoryPort noteRepository,
            PatientValidatorPort patientValidator,
            PatientAccessAuthorizationPort accessAuthorizationPort,
            DocumentSignatureRepositoryPort signatureRepository) {
        return new ClinicalNoteService(noteRepository, patientValidator, accessAuthorizationPort, signatureRepository);
    }

    @Bean
    public TemporaryRecordShareService temporaryRecordShareService(
            TemporaryRecordShareRepositoryPort repository,
            ClinicalNoteRepositoryPort noteRepository,
            PatientLookupPort patientLookup,
            ClinicLookupPort clinicLookup,
            PatientAccessAuthorizationPort accessAuthorizationPort) {
        return new TemporaryRecordShareService(repository, noteRepository, patientLookup, clinicLookup, accessAuthorizationPort);
    }

    @Bean
    public RecordAccessLogService recordAccessLogService(
            RecordAccessLogRepositoryPort logRepository,
            RecordAccessLogOutboxPort outbox,
            PatientValidatorPort patientValidator,
            PatientAccessAuthorizationPort accessAuthorizationPort) {
        return new RecordAccessLogService(logRepository, outbox, patientValidator, accessAuthorizationPort);
    }

    @Bean
    public PrivacyConsentService privacyConsentService(
            PrivacyConsentRepositoryPort consentRepository,
            PatientValidatorPort patientValidator,
            PatientAccessAuthorizationPort accessAuthorizationPort) {
        return new PrivacyConsentService(consentRepository, patientValidator, accessAuthorizationPort);
    }

    @Bean
    @ConditionalOnMissingBean(MedicalHistoryTemplateMapper.class)
    public MedicalHistoryTemplateMapper medicalHistoryTemplateMapper() {
        return new MedicalHistoryTemplateMapper() {
            @Override
            public MedicalHistoryTemplateEntity toEntity(MedicalHistoryTemplate domain) {
                if (domain == null) {
                    return null;
                }
                return MedicalHistoryTemplateEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .name(domain.getName())
                        .description(domain.getDescription())
                        .schemaJson(domain.getSchemaJson())
                        .active(domain.isActive())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public MedicalHistoryTemplate toDomain(MedicalHistoryTemplateEntity entity) {
                if (entity == null) {
                    return null;
                }
                return MedicalHistoryTemplate.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .name(entity.getName())
                        .description(entity.getDescription())
                        .schemaJson(entity.getSchemaJson())
                        .active(entity.isActive())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(MedicalHistoryMapper.class)
    public MedicalHistoryMapper medicalHistoryMapper() {
        return new MedicalHistoryMapper() {
            @Override
            public MedicalHistoryEntity toEntity(MedicalHistory domain) {
                if (domain == null) {
                    return null;
                }
                return MedicalHistoryEntity.builder()
                        .id(domain.getId())
                        .patientId(domain.getPatientId())
                        .clinicId(domain.getClinicId())
                        .templateId(domain.getTemplateId())
                        .answersJson(domain.getAnswersJson())
                        .version(domain.getVersion())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public MedicalHistory toDomain(MedicalHistoryEntity entity) {
                if (entity == null) {
                    return null;
                }
                return MedicalHistory.builder()
                        .id(entity.getId())
                        .patientId(entity.getPatientId())
                        .clinicId(entity.getClinicId())
                        .templateId(entity.getTemplateId())
                        .answersJson(entity.getAnswersJson())
                        .version(entity.getVersion())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(ClinicalNoteMapper.class)
    public ClinicalNoteMapper clinicalNoteMapper() {
        return new ClinicalNoteMapper() {
            @Override
            public ClinicalNoteEntity toEntity(ClinicalNote domain) {
                if (domain == null) {
                    return null;
                }
                VitalSigns vitalSigns = domain.getVitalSigns();
                return ClinicalNoteEntity.builder()
                        .id(domain.getId())
                        .patientId(domain.getPatientId())
                        .clinicId(domain.getClinicId())
                        .doctorId(domain.getDoctorId())
                        .subjective(domain.getSubjective())
                        .objective(domain.getObjective())
                        .assessment(domain.getAssessment())
                        .plan(domain.getPlan())
                        .status(toStatusValue(domain.getStatus()))
                        .authoredByExternalUserId(domain.getAuthoredByExternalUserId())
                        .signedAt(domain.getSignedAt())
                        .signedByUserId(domain.getSignedByUserId())
                        .documentHash(domain.getDocumentHash())
                        .vitalTemp(vitalSigns == null ? null : vitalSigns.temperature())
                        .vitalBp(vitalSigns == null ? null : vitalSigns.bloodPressure())
                        .vitalHr(vitalSigns == null ? null : vitalSigns.heartRate())
                        .vitalRr(vitalSigns == null ? null : vitalSigns.respiratoryRate())
                        .vitalWeight(vitalSigns == null ? null : vitalSigns.weight())
                        .vitalHeight(vitalSigns == null ? null : vitalSigns.height())
                        .vitalBmi(vitalSigns == null ? null : vitalSigns.bmi())
                        .vitalO2(vitalSigns == null ? null : vitalSigns.oxygenSaturation())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public ClinicalNote toDomain(ClinicalNoteEntity entity) {
                if (entity == null) {
                    return null;
                }
                return ClinicalNote.builder()
                        .id(entity.getId())
                        .patientId(entity.getPatientId())
                        .clinicId(entity.getClinicId())
                        .doctorId(entity.getDoctorId())
                        .subjective(entity.getSubjective())
                        .objective(entity.getObjective())
                        .vitalSigns(mapVitalSigns(entity))
                        .assessment(entity.getAssessment())
                        .plan(entity.getPlan())
                        .status(toNoteStatus(entity.getStatus()))
                        .authoredByExternalUserId(entity.getAuthoredByExternalUserId())
                        .signedAt(entity.getSignedAt())
                        .signedByUserId(entity.getSignedByUserId())
                        .documentHash(entity.getDocumentHash())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(DocumentSignatureMapper.class)
    public DocumentSignatureMapper documentSignatureMapper() {
        return new DocumentSignatureMapper() {
            @Override
            public DocumentSignatureEntity toEntity(DocumentSignature domain) {
                if (domain == null) {
                    return null;
                }
                return DocumentSignatureEntity.builder()
                        .id(domain.getId())
                        .documentType(domain.getDocumentType().name())
                        .documentId(domain.getDocumentId())
                        .clinicId(domain.getClinicId())
                        .patientId(domain.getPatientId())
                        .signerType(domain.getSignerType().name())
                        .signerUserId(domain.getSignerUserId())
                        .signerName(domain.getSignerName())
                        .signatureFieldId(domain.getSignatureFieldId())
                        .signatureFieldLabel(domain.getSignatureFieldLabel())
                        .signatureImageHash(domain.getSignatureImageHash())
                        .documentHash(domain.getDocumentHash())
                        .ipAddress(domain.getIpAddress())
                        .userAgent(domain.getUserAgent())
                        .status(domain.getStatus().name())
                        .signedAt(domain.getSignedAt())
                        .createdAt(domain.getCreatedAt())
                        .build();
            }

            @Override
            public DocumentSignature toDomain(DocumentSignatureEntity entity) {
                if (entity == null) {
                    return null;
                }
                return DocumentSignature.builder()
                        .id(entity.getId())
                        .documentType(SignatureDocumentType.valueOf(entity.getDocumentType()))
                        .documentId(entity.getDocumentId())
                        .clinicId(entity.getClinicId())
                        .patientId(entity.getPatientId())
                        .signerType(SignerType.valueOf(entity.getSignerType()))
                        .signerUserId(entity.getSignerUserId())
                        .signerName(entity.getSignerName())
                        .signatureFieldId(entity.getSignatureFieldId())
                        .signatureFieldLabel(entity.getSignatureFieldLabel())
                        .signatureImageHash(entity.getSignatureImageHash())
                        .documentHash(entity.getDocumentHash())
                        .ipAddress(entity.getIpAddress())
                        .userAgent(entity.getUserAgent())
                        .status(DocumentSignatureStatus.valueOf(entity.getStatus()))
                        .signedAt(entity.getSignedAt())
                        .createdAt(entity.getCreatedAt())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(RecordAccessLogMapper.class)
    public RecordAccessLogMapper recordAccessLogMapper() {
        return new RecordAccessLogMapper() {
            @Override
            public RecordAccessLogEntity toEntity(RecordAccessLog domain) {
                if (domain == null) {
                    return null;
                }
                return RecordAccessLogEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .patientId(domain.getPatientId())
                        .userId(domain.getUserId())
                        .userName(domain.getUserName())
                        .resourceType(domain.getResourceType())
                        .resourceId(domain.getResourceId())
                        .actionType(domain.getActionType())
                        .ipAddress(domain.getIpAddress())
                        .userAgent(domain.getUserAgent())
                        .createdAt(domain.getCreatedAt())
                        .build();
            }

            @Override
            public RecordAccessLog toDomain(RecordAccessLogEntity entity) {
                if (entity == null) {
                    return null;
                }
                return RecordAccessLog.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .patientId(entity.getPatientId())
                        .userId(entity.getUserId())
                        .userName(entity.getUserName())
                        .resourceType(entity.getResourceType())
                        .resourceId(entity.getResourceId())
                        .actionType(entity.getActionType())
                        .ipAddress(entity.getIpAddress())
                        .userAgent(entity.getUserAgent())
                        .createdAt(entity.getCreatedAt())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(MedicalHistoryVersionMapper.class)
    public MedicalHistoryVersionMapper medicalHistoryVersionMapper() {
        return new MedicalHistoryVersionMapper() {
            @Override
            public MedicalHistoryVersionEntity toEntity(MedicalHistoryVersion domain) {
                if (domain == null) {
                    return null;
                }
                return MedicalHistoryVersionEntity.builder()
                        .id(domain.getId())
                        .medicalHistoryId(domain.getMedicalHistoryId())
                        .version(domain.getVersion())
                        .answersJson(domain.getAnswersJson())
                        .changedByUserId(domain.getChangedByUserId())
                        .changedByUserName(domain.getChangedByUserName())
                        .ipAddress(domain.getIpAddress())
                        .userAgent(domain.getUserAgent())
                        .createdAt(domain.getCreatedAt())
                        .build();
            }

            @Override
            public MedicalHistoryVersion toDomain(MedicalHistoryVersionEntity entity) {
                if (entity == null) {
                    return null;
                }
                return MedicalHistoryVersion.builder()
                        .id(entity.getId())
                        .medicalHistoryId(entity.getMedicalHistoryId())
                        .version(entity.getVersion())
                        .answersJson(entity.getAnswersJson())
                        .changedByUserId(entity.getChangedByUserId())
                        .changedByUserName(entity.getChangedByUserName())
                        .ipAddress(entity.getIpAddress())
                        .userAgent(entity.getUserAgent())
                        .createdAt(entity.getCreatedAt())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(PrivacyConsentMapper.class)
    public PrivacyConsentMapper privacyConsentMapper() {
        return new PrivacyConsentMapper() {
            @Override
            public PrivacyConsentEntity toEntity(PrivacyConsent domain) {
                if (domain == null) {
                    return null;
                }
                return PrivacyConsentEntity.builder()
                        .id(domain.getId())
                        .patientId(domain.getPatientId())
                        .clinicId(domain.getClinicId())
                        .privacyNoticeText(domain.getPrivacyNoticeText())
                        .documentHash(domain.getDocumentHash())
                        .signerName(domain.getSignerName())
                        .signatureImage(domain.getSignatureImage())
                        .signatureImageHash(domain.getSignatureImageHash())
                        .ipAddress(domain.getIpAddress())
                        .userAgent(domain.getUserAgent())
                        .signedAt(domain.getSignedAt())
                        .createdAt(domain.getCreatedAt())
                        .build();
            }

            @Override
            public PrivacyConsent toDomain(PrivacyConsentEntity entity) {
                if (entity == null) {
                    return null;
                }
                return PrivacyConsent.builder()
                        .id(entity.getId())
                        .patientId(entity.getPatientId())
                        .clinicId(entity.getClinicId())
                        .privacyNoticeText(entity.getPrivacyNoticeText())
                        .documentHash(entity.getDocumentHash())
                        .signerName(entity.getSignerName())
                        .signatureImage(entity.getSignatureImage())
                        .signatureImageHash(entity.getSignatureImageHash())
                        .ipAddress(entity.getIpAddress())
                        .userAgent(entity.getUserAgent())
                        .signedAt(entity.getSignedAt())
                        .createdAt(entity.getCreatedAt())
                        .build();
            }
        };
    }

    private String toStatusValue(NoteStatus status) {
        return status == null ? null : status.name();
    }

    private NoteStatus toNoteStatus(String status) {
        return status == null || status.isBlank() ? null : NoteStatus.valueOf(status);
    }
}
