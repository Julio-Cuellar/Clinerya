package com.jclinical.staff.infra.config;

import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.DoctorCredentialStatus;
import com.jclinical.staff.domain.model.DoctorProfile;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.staff.domain.ports.in.ManageStaffOperationsUseCase;
import com.jclinical.staff.domain.ports.out.ClinicStaffInvitationRepositoryPort;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.DoctorProfileRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffActivityRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffAttendanceRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPayrollLineRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPayrollPeriodRepositoryPort;
import com.jclinical.staff.domain.ports.out.UserDirectoryPort;
import com.jclinical.staff.domain.ports.out.StaffPermissionCheckerPort;
import com.jclinical.staff.domain.ports.out.StaffPermissionOverrideRepositoryPort;
import com.jclinical.staff.domain.ports.out.PayrollAccountingPort;
import com.jclinical.staff.domain.service.ClinicStaffService;
import com.jclinical.staff.domain.service.StaffOperationsService;
import com.jclinical.staff.infra.adapters.out.ClinicStaffEntity;
import com.jclinical.staff.infra.adapters.out.ClinicStaffMapper;
import com.jclinical.staff.infra.adapters.out.DoctorProfileEntity;
import com.jclinical.staff.infra.adapters.out.DoctorProfileMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StaffDomainConfig {

    @Bean
    @ConditionalOnMissingBean(ManageClinicStaffUseCase.class)
    public ManageClinicStaffUseCase manageClinicStaffUseCase(
            ClinicStaffRepositoryPort clinicStaffRepository,
            DoctorProfileRepositoryPort doctorProfileRepository,
            UserDirectoryPort userDirectory,
            ClinicStaffInvitationRepositoryPort clinicStaffInvitationRepository,
            StaffPermissionOverrideRepositoryPort permissionOverrideRepository) {
        return new ClinicStaffService(
                clinicStaffRepository,
                doctorProfileRepository,
                userDirectory,
                clinicStaffInvitationRepository,
                permissionOverrideRepository);
    }

    @Bean
    @ConditionalOnMissingBean(ManageStaffOperationsUseCase.class)
    public ManageStaffOperationsUseCase manageStaffOperationsUseCase(
            ClinicStaffRepositoryPort clinicStaffRepository,
            StaffAttendanceRepositoryPort attendanceRepository,
            StaffActivityRepositoryPort activityRepository,
            StaffPayrollPeriodRepositoryPort payrollPeriodRepository,
            StaffPayrollLineRepositoryPort payrollLineRepository,
            PayrollAccountingPort payrollAccounting,
            StaffPermissionCheckerPort staffPermissionChecker) {
        return new StaffOperationsService(
                clinicStaffRepository,
                attendanceRepository,
                activityRepository,
                payrollPeriodRepository,
                payrollLineRepository,
                payrollAccounting,
                staffPermissionChecker);
    }

    @Bean
    @ConditionalOnMissingBean(ClinicStaffMapper.class)
    public ClinicStaffMapper clinicStaffMapper() {
        return new ClinicStaffMapper() {
            @Override
            public ClinicStaffEntity toEntity(ClinicStaff domain) {
                if (domain == null) {
                    return null;
                }
                return ClinicStaffEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .userId(domain.getUserId())
                        .role(domain.getRole() != null ? domain.getRole().name() : null)
                        .employeeCode(domain.getEmployeeCode())
                        .hireDate(domain.getHireDate())
                        .endDate(domain.getEndDate())
                        .notes(domain.getNotes())
                        .active(domain.isActive())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public ClinicStaff toDomain(ClinicStaffEntity entity) {
                if (entity == null) {
                    return null;
                }
                return ClinicStaff.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .userId(entity.getUserId())
                        .role(entity.getRole() != null ? StaffRole.valueOf(entity.getRole()) : null)
                        .employeeCode(entity.getEmployeeCode())
                        .hireDate(entity.getHireDate())
                        .endDate(entity.getEndDate())
                        .notes(entity.getNotes())
                        .active(entity.isActive())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(DoctorProfileMapper.class)
    public DoctorProfileMapper doctorProfileMapper() {
        return new DoctorProfileMapper() {
            @Override
            public DoctorProfileEntity toEntity(DoctorProfile domain) {
                if (domain == null) {
                    return null;
                }
                return DoctorProfileEntity.builder()
                        .id(domain.getId())
                        .clinicStaffId(domain.getClinicStaffId())
                        .clinicId(domain.getClinicId())
                        .cedulaProfesional(domain.getCedulaProfesional())
                        .cedulaEspecialidad(domain.getCedulaEspecialidad())
                        .especialidad(domain.getEspecialidad())
                        .subEspecialidad(domain.getSubEspecialidad())
                        .universidadEgreso(domain.getUniversidadEgreso())
                        .anioEgreso(domain.getAnioEgreso())
                        .credentialStatus(domain.getCredentialStatus() != null ? domain.getCredentialStatus().name() : null)
                        .verifiedAt(domain.getVerifiedAt())
                        .verifiedByUserId(domain.getVerifiedByUserId())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public DoctorProfile toDomain(DoctorProfileEntity entity) {
                if (entity == null) {
                    return null;
                }
                return DoctorProfile.builder()
                        .id(entity.getId())
                        .clinicStaffId(entity.getClinicStaffId())
                        .clinicId(entity.getClinicId())
                        .cedulaProfesional(entity.getCedulaProfesional())
                        .cedulaEspecialidad(entity.getCedulaEspecialidad())
                        .especialidad(entity.getEspecialidad())
                        .subEspecialidad(entity.getSubEspecialidad())
                        .universidadEgreso(entity.getUniversidadEgreso())
                        .anioEgreso(entity.getAnioEgreso())
                        .credentialStatus(entity.getCredentialStatus() != null ? DoctorCredentialStatus.valueOf(entity.getCredentialStatus()) : null)
                        .verifiedAt(entity.getVerifiedAt())
                        .verifiedByUserId(entity.getVerifiedByUserId())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };
    }
}
