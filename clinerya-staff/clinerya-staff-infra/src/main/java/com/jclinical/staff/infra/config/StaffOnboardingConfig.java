package com.jclinical.staff.infra.config;

import com.jclinical.staff.domain.ports.in.ManageStaffOnboardingUseCase;
import com.jclinical.staff.domain.ports.out.StaffCompensationRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffInvitationCompensationRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPermissionCheckerPort;
import com.jclinical.staff.domain.service.StaffOnboardingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StaffOnboardingConfig {

    @Bean
    @ConditionalOnMissingBean(ManageStaffOnboardingUseCase.class)
    public ManageStaffOnboardingUseCase manageStaffOnboardingUseCase(
            StaffPermissionCheckerPort staffPermissionChecker,
            StaffInvitationCompensationRepositoryPort invitationCompensationRepository,
            StaffCompensationRepositoryPort compensationRepository) {
        return new StaffOnboardingService(
                staffPermissionChecker,
                invitationCompensationRepository,
                compensationRepository);
    }
}
