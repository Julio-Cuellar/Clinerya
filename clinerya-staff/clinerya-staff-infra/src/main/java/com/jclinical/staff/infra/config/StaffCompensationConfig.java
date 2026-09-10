package com.jclinical.staff.infra.config;

import com.jclinical.staff.domain.ports.in.ManageStaffCompensationUseCase;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffCompensationRepositoryPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.staff.domain.service.StaffCompensationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StaffCompensationConfig {

    @Bean
    @ConditionalOnMissingBean(ManageStaffCompensationUseCase.class)
    public ManageStaffCompensationUseCase manageStaffCompensationUseCase(
            StaffCompensationRepositoryPort staffCompensationRepository,
            ClinicStaffRepositoryPort clinicStaffRepository,
            StaffPermissionCheckerPort staffPermissionChecker) {
        return new StaffCompensationService(
                staffCompensationRepository,
                clinicStaffRepository,
                staffPermissionChecker);
    }
}
