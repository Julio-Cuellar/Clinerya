package com.jclinical.clinics.infra.config;

import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.clinics.domain.model.ClinicRoom;
import com.jclinical.clinics.domain.model.ClinicRoomStaffAssignment;
import com.jclinical.clinics.domain.ports.in.GetClinicSettingsUseCase;
import com.jclinical.clinics.domain.ports.in.ManageClinicUseCase;
import com.jclinical.clinics.domain.ports.in.OnboardClinicUseCase;
import com.jclinical.clinics.domain.ports.out.ClinicRepositoryPort;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.DoctorProfileRepositoryPort;

import com.jclinical.clinics.domain.service.ManageClinicService;
import com.jclinical.clinics.domain.service.OnboardClinicService;
import com.jclinical.clinics.infra.adapters.in.web.dto.ClinicResponse;
import com.jclinical.clinics.infra.adapters.out.ClinicEntity;
import com.jclinical.clinics.infra.adapters.out.ClinicMapper;
import com.jclinical.clinics.infra.adapters.out.ClinicRoomEntity;
import com.jclinical.clinics.infra.adapters.out.ClinicRoomMapper;
import com.jclinical.clinics.infra.adapters.out.ClinicRoomStaffAssignmentEntity;
import com.jclinical.clinics.infra.adapters.out.ClinicRoomStaffAssignmentMapper;
import com.jclinical.clinics.infra.adapters.in.web.dto.ClinicRoomResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClinicDomainConfig {

    @Bean
    public OnboardClinicUseCase onboardClinicUseCase(
            ClinicRepositoryPort clinicRepository,
            ClinicStaffRepositoryPort clinicStaffRepository,
            DoctorProfileRepositoryPort doctorProfileRepository) {
        return new OnboardClinicService(clinicRepository, clinicStaffRepository, doctorProfileRepository);
    }

    @Bean
    @ConditionalOnMissingBean(ManageClinicUseCase.class)
    public ManageClinicUseCase manageClinicUseCase(
            ClinicRepositoryPort clinicRepository,
            ClinicStaffRepositoryPort clinicStaffRepository,
            DoctorProfileRepositoryPort doctorProfileRepository) {
        return new ManageClinicService(clinicRepository, clinicStaffRepository, doctorProfileRepository);
    }

    @Bean
    @ConditionalOnMissingBean(GetClinicSettingsUseCase.class)
    public GetClinicSettingsUseCase getClinicSettingsUseCase(ManageClinicUseCase manageClinicUseCase) {
        return (GetClinicSettingsUseCase) manageClinicUseCase;
    }

    @Bean
    @ConditionalOnMissingBean(com.jclinical.clinics.domain.ports.in.ManageClinicRoomsUseCase.class)
    public com.jclinical.clinics.domain.ports.in.ManageClinicRoomsUseCase manageClinicRoomsUseCase(
            com.jclinical.clinics.domain.ports.out.ClinicRoomRepositoryPort clinicRoomRepository,
            com.jclinical.clinics.domain.ports.out.ClinicRoomStaffAssignmentRepositoryPort assignmentRepository,
            ClinicStaffRepositoryPort clinicStaffRepository,
            com.jclinical.core.security.StaffPermissionCheckerPort permissionChecker) {
        return new com.jclinical.clinics.domain.service.ClinicRoomService(
                clinicRoomRepository,
                assignmentRepository,
                clinicStaffRepository,
                permissionChecker);
    }

    @Bean
    @ConditionalOnMissingBean(ClinicRoomMapper.class)
    public ClinicRoomMapper clinicRoomMapper() {
        return new ClinicRoomMapper() {
            @Override
            public ClinicRoomEntity toEntity(ClinicRoom domain) {
                if (domain == null) return null;
                return ClinicRoomEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .name(domain.getName())
                        .code(domain.getCode())
                        .colorHex(domain.getColorHex())
                        .description(domain.getDescription())
                        .active(domain.isActive())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public ClinicRoom toDomain(ClinicRoomEntity entity) {
                if (entity == null) return null;
                return ClinicRoom.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .name(entity.getName())
                        .code(entity.getCode())
                        .colorHex(entity.getColorHex())
                        .description(entity.getDescription())
                        .active(entity.isActive())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }

            @Override
            public ClinicRoomResponse toResponse(ClinicRoom domain) {
                if (domain == null) return null;
                return new ClinicRoomResponse(
                        domain.getId(),
                        domain.getClinicId(),
                        domain.getName(),
                        domain.getCode(),
                        domain.getColorHex(),
                        domain.getDescription(),
                        domain.isActive(),
                        domain.getCreatedAt(),
                        domain.getUpdatedAt());
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(ClinicRoomStaffAssignmentMapper.class)
    public ClinicRoomStaffAssignmentMapper clinicRoomStaffAssignmentMapper() {
        return new ClinicRoomStaffAssignmentMapper() {
            @Override
            public ClinicRoomStaffAssignmentEntity toEntity(ClinicRoomStaffAssignment domain) {
                if (domain == null) return null;
                return ClinicRoomStaffAssignmentEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .roomId(domain.getRoomId())
                        .staffId(domain.getStaffId())
                        .active(domain.isActive())
                        .assignedAt(domain.getAssignedAt())
                        .unassignedAt(domain.getUnassignedAt())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public ClinicRoomStaffAssignment toDomain(ClinicRoomStaffAssignmentEntity entity) {
                if (entity == null) return null;
                return ClinicRoomStaffAssignment.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .roomId(entity.getRoomId())
                        .staffId(entity.getStaffId())
                        .active(entity.isActive())
                        .assignedAt(entity.getAssignedAt())
                        .unassignedAt(entity.getUnassignedAt())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(ClinicMapper.class)
    public ClinicMapper clinicMapper() {
        return new ClinicMapper() {
            @Override
            public ClinicEntity toEntity(Clinic domain) {
                if (domain == null) {
                    return null;
                }
                return ClinicEntity.builder()
                        .id(domain.getId())
                        .organizationId(domain.getOrganizationId())
                        .ownerUserId(domain.getOwnerUserId())
                        .name(domain.getName())
                        .legalName(domain.getLegalName())
                        .rfc(domain.getRfc())
                        .taxRegimeCode(domain.getTaxRegimeCode())
                        .addressStreet(domain.getAddressStreet())
                        .addressColonia(domain.getAddressColonia())
                        .addressMunicipality(domain.getAddressMunicipality())
                        .addressState(domain.getAddressState())
                        .addressZip(domain.getAddressZip())
                        .phone(domain.getPhone())
                        .email(domain.getEmail())
                        .logoUrl(domain.getLogoUrl())
                        .timezone(domain.getTimezone())
                        .privacyNoticeUrl(domain.getPrivacyNoticeUrl())
                        .cofeprisPermitNumber(domain.getCofeprisPermitNumber())
                        .responsibleDoctorName(domain.getResponsibleDoctorName())
                        .responsibleDoctorProfessionalLicense(domain.getResponsibleDoctorProfessionalLicense())
                        .dataProcessorAgreedAt(domain.getDataProcessorAgreedAt())
                        .legalRepresentativeStaffId(domain.getLegalRepresentativeStaffId())
                        .active(domain.isActive())
                        .materialReservationLeadDays(domain.getMaterialReservationLeadDays())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public Clinic toDomain(ClinicEntity entity) {
                if (entity == null) {
                    return null;
                }
                return Clinic.builder()
                        .id(entity.getId())
                        .organizationId(entity.getOrganizationId())
                        .ownerUserId(entity.getOwnerUserId())
                        .name(entity.getName())
                        .legalName(entity.getLegalName())
                        .rfc(entity.getRfc())
                        .taxRegimeCode(entity.getTaxRegimeCode())
                        .addressStreet(entity.getAddressStreet())
                        .addressColonia(entity.getAddressColonia())
                        .addressMunicipality(entity.getAddressMunicipality())
                        .addressState(entity.getAddressState())
                        .addressZip(entity.getAddressZip())
                        .phone(entity.getPhone())
                        .email(entity.getEmail())
                        .logoUrl(entity.getLogoUrl())
                        .timezone(entity.getTimezone())
                        .privacyNoticeUrl(entity.getPrivacyNoticeUrl())
                        .cofeprisPermitNumber(entity.getCofeprisPermitNumber())
                        .responsibleDoctorName(entity.getResponsibleDoctorName())
                        .responsibleDoctorProfessionalLicense(entity.getResponsibleDoctorProfessionalLicense())
                        .dataProcessorAgreedAt(entity.getDataProcessorAgreedAt())
                        .legalRepresentativeStaffId(entity.getLegalRepresentativeStaffId())
                        .active(entity.isActive())
                        .materialReservationLeadDays(entity.getMaterialReservationLeadDays())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }

            @Override
            public ClinicResponse toResponse(Clinic domain) {
                if (domain == null) {
                    return null;
                }
                return new ClinicResponse(
                        domain.getId(),
                        domain.getOrganizationId(),
                        domain.getOwnerUserId(),
                        domain.getName(),
                        domain.getLegalName(),
                        domain.getRfc(),
                        domain.getTaxRegimeCode(),
                        domain.getAddressStreet(),
                        domain.getAddressColonia(),
                        domain.getAddressMunicipality(),
                        domain.getAddressState(),
                        domain.getAddressZip(),
                        domain.getPhone(),
                        domain.getEmail(),
                        domain.getLogoUrl(),
                        domain.getTimezone(),
                        domain.getPrivacyNoticeUrl(),
                        domain.getCofeprisPermitNumber(),
                        domain.getResponsibleDoctorName(),
                        domain.getResponsibleDoctorProfessionalLicense(),
                        domain.getDataProcessorAgreedAt(),
                        domain.getLegalRepresentativeStaffId(),
                        domain.isActive(),
                        domain.getMaterialReservationLeadDays(),
                        domain.getCreatedAt(),
                        domain.getUpdatedAt()
                );
            }
        };
    }

}
