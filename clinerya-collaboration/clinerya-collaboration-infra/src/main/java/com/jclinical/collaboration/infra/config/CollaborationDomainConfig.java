package com.jclinical.collaboration.infra.config;

import com.jclinical.collaboration.domain.model.AccessLevel;
import com.jclinical.collaboration.domain.model.ExternalAccessGrant;
import com.jclinical.collaboration.domain.model.ExternalAccessStatus;
import com.jclinical.collaboration.domain.ports.out.ClinicStaffDirectoryPort;
import com.jclinical.collaboration.domain.ports.out.ExternalAccessGrantRepositoryPort;
import com.jclinical.collaboration.domain.ports.out.PatientDirectoryPort;
import com.jclinical.collaboration.domain.ports.out.UserDirectoryPort;
import com.jclinical.collaboration.domain.service.ExternalAccessService;
import com.jclinical.collaboration.infra.adapters.out.persistence.ExternalAccessGrantEntity;
import com.jclinical.collaboration.infra.adapters.out.persistence.ExternalAccessGrantMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CollaborationDomainConfig {

    @Bean
    public ExternalAccessService externalAccessService(
            ExternalAccessGrantRepositoryPort repository,
            ClinicStaffDirectoryPort staffDirectory,
            UserDirectoryPort userDirectory,
            PatientDirectoryPort patientDirectory) {
        return new ExternalAccessService(repository, staffDirectory, userDirectory, patientDirectory);
    }

    @Bean
    @ConditionalOnMissingBean(ExternalAccessGrantMapper.class)
    public ExternalAccessGrantMapper externalAccessGrantMapper() {
        return new ExternalAccessGrantMapper() {
            @Override
            public ExternalAccessGrantEntity toEntity(ExternalAccessGrant domain) {
                if (domain == null) {
                    return null;
                }
                return ExternalAccessGrantEntity.builder()
                        .id(domain.getId())
                        .sourceClinicId(domain.getSourceClinicId())
                        .patientId(domain.getPatientId())
                        .invitedByStaffId(domain.getInvitedByStaffId())
                        .externalUserId(domain.getExternalUserId())
                        .invitedEmail(domain.getInvitedEmail())
                        .accessLevel(domain.getAccessLevel().name())
                        .status(domain.getStatus().name())
                        .createdAt(domain.getCreatedAt())
                        .respondedAt(domain.getRespondedAt())
                        .revokedAt(domain.getRevokedAt())
                        .build();
            }

            @Override
            public ExternalAccessGrant toDomain(ExternalAccessGrantEntity entity) {
                if (entity == null) {
                    return null;
                }
                return ExternalAccessGrant.builder()
                        .id(entity.getId())
                        .sourceClinicId(entity.getSourceClinicId())
                        .patientId(entity.getPatientId())
                        .invitedByStaffId(entity.getInvitedByStaffId())
                        .externalUserId(entity.getExternalUserId())
                        .invitedEmail(entity.getInvitedEmail())
                        .accessLevel(AccessLevel.valueOf(entity.getAccessLevel()))
                        .status(ExternalAccessStatus.valueOf(entity.getStatus()))
                        .createdAt(entity.getCreatedAt())
                        .respondedAt(entity.getRespondedAt())
                        .revokedAt(entity.getRevokedAt())
                        .build();
            }
        };
    }
}
