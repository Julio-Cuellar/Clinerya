package com.jclinical.integrations.infra.config;

import com.jclinical.integrations.domain.model.CalendarCredentials;
import com.jclinical.integrations.domain.ports.out.CalendarCredentialsRepositoryPort;
import com.jclinical.integrations.domain.ports.out.GoogleOAuthPort;
import com.jclinical.integrations.domain.ports.out.StateCodecPort;
import com.jclinical.integrations.domain.service.CalendarIntegrationService;
import com.jclinical.integrations.infra.adapters.out.persistence.StaffCalendarCredentialsEntity;
import com.jclinical.integrations.infra.adapters.out.persistence.StaffCalendarCredentialsMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegrationsDomainConfig {

    @Bean
    public CalendarIntegrationService calendarIntegrationService(
            CalendarCredentialsRepositoryPort credentialsRepository,
            GoogleOAuthPort oAuthPort,
            StateCodecPort stateCodec) {
        return new CalendarIntegrationService(credentialsRepository, oAuthPort, stateCodec);
    }

    @Bean
    public com.jclinical.integrations.domain.ports.in.ManageExternalCalendarEventsUseCase manageExternalCalendarEventsUseCase(
            com.jclinical.integrations.domain.ports.out.ExternalCalendarEventRepositoryPort repositoryPort) {
        return new com.jclinical.integrations.domain.service.ManageExternalCalendarEventsService(repositoryPort);
    }

    @Bean
    @ConditionalOnMissingBean(StaffCalendarCredentialsMapper.class)
    public StaffCalendarCredentialsMapper staffCalendarCredentialsMapper() {
        return new StaffCalendarCredentialsMapper() {
            @Override
            public StaffCalendarCredentialsEntity toEntity(CalendarCredentials domain) {
                if (domain == null) {
                    return null;
                }
                return StaffCalendarCredentialsEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .staffId(domain.getStaffId())
                        .googleAccountEmail(domain.getGoogleAccountEmail())
                        .accessToken(domain.getAccessToken())
                        .refreshToken(domain.getRefreshToken())
                        .tokenExpiry(domain.getTokenExpiry())
                        .googleCalendarId(domain.getGoogleCalendarId())
                        .calendarSyncToken(domain.getCalendarSyncToken())
                        .importPastEvents(domain.isImportPastEvents())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public CalendarCredentials toDomain(StaffCalendarCredentialsEntity entity) {
                if (entity == null) {
                    return null;
                }
                return CalendarCredentials.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .staffId(entity.getStaffId())
                        .googleAccountEmail(entity.getGoogleAccountEmail())
                        .accessToken(entity.getAccessToken())
                        .refreshToken(entity.getRefreshToken())
                        .tokenExpiry(entity.getTokenExpiry())
                        .googleCalendarId(entity.getGoogleCalendarId())
                        .calendarSyncToken(entity.getCalendarSyncToken())
                        .importPastEvents(entity.isImportPastEvents())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };

    }
}

