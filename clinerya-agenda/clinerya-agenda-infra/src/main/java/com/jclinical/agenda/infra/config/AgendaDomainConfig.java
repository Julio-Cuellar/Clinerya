package com.jclinical.agenda.infra.config;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.model.RoomBlock;
import com.jclinical.agenda.domain.model.WaitingListEntry;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.ClinicScheduleRepositoryPort;
import com.jclinical.agenda.domain.ports.out.ClinicSettingsPort;
import com.jclinical.agenda.domain.ports.out.PatientValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.agenda.domain.service.AppointmentService;
import com.jclinical.agenda.domain.service.ClinicScheduleService;
import com.jclinical.agenda.domain.service.MaterialReservationSchedulingService;
import com.jclinical.agenda.infra.adapters.out.persistence.AppointmentEntity;
import com.jclinical.agenda.infra.adapters.out.persistence.AppointmentMapper;
import com.jclinical.agenda.infra.adapters.out.persistence.ClinicScheduleEntity;
import com.jclinical.agenda.infra.adapters.out.persistence.ClinicScheduleMapper;
import com.jclinical.agenda.infra.adapters.out.persistence.RoomBlockEntity;
import com.jclinical.agenda.infra.adapters.out.persistence.RoomBlockMapper;
import com.jclinical.agenda.infra.adapters.out.persistence.WaitingListEntity;
import com.jclinical.agenda.infra.adapters.out.persistence.WaitingListMapper;
import com.jclinical.agenda.infra.adapters.in.web.dto.RoomBlockResponse;
import com.jclinical.agenda.infra.adapters.in.web.dto.WaitingListResponse;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgendaDomainConfig {

    @Bean
    public ClinicScheduleService clinicScheduleService(ClinicScheduleRepositoryPort scheduleRepository,
                                                       StaffPermissionCheckerPort permissionChecker) {
        return new ClinicScheduleService(scheduleRepository, permissionChecker);
    }

    @Bean
    public AppointmentService appointmentService(
            AppointmentRepositoryPort appointmentRepository,
            ClinicScheduleService clinicScheduleService,
            PatientValidatorPort patientValidator,
            StaffValidatorPort staffValidator,
            QuotationValidatorPort quotationValidator,
            MaterialReservationSchedulingService materialReservationSchedulingService,
            DomainEventPublisherPort eventPublisher,
            com.jclinical.agenda.domain.ports.out.RoomBlockRepositoryPort roomBlockRepository,
            com.jclinical.agenda.domain.ports.out.RoomValidatorPort roomValidator,
            StaffPermissionCheckerPort permissionChecker,
            com.jclinical.agenda.domain.ports.out.SlotHoldRepositoryPort slotHoldRepository,
            com.jclinical.agenda.domain.ports.out.ServiceCatalogPort serviceCatalog) {
        AppointmentService service = new AppointmentService(
                appointmentRepository, clinicScheduleService, patientValidator, staffValidator, quotationValidator,
                materialReservationSchedulingService, eventPublisher, roomBlockRepository, roomValidator, permissionChecker,
                slotHoldRepository);
        service.setServiceCatalog(serviceCatalog);
        return service;
    }

    @Bean
    public com.jclinical.agenda.domain.service.OnlineBookingSettingsService onlineBookingSettingsService(
            com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsRepositoryPort settingsRepository,
            StaffValidatorPort staffValidator,
            StaffPermissionCheckerPort permissionChecker) {
        return new com.jclinical.agenda.domain.service.OnlineBookingSettingsService(
                settingsRepository, staffValidator, permissionChecker);
    }

    @Bean
    public com.jclinical.agenda.domain.service.OnlineBookingService onlineBookingService(
            ClinicScheduleService clinicScheduleService,
            AppointmentRepositoryPort appointmentRepository,
            com.jclinical.agenda.domain.ports.out.SlotHoldRepositoryPort slotHoldRepository,
            com.jclinical.agenda.domain.service.OnlineBookingSettingsService settings,
            StaffValidatorPort staffValidator,
            AppointmentService appointmentService) {
        return new com.jclinical.agenda.domain.service.OnlineBookingService(clinicScheduleService, appointmentRepository,
                slotHoldRepository, settings, staffValidator, appointmentService, java.time.Clock.systemDefaultZone());
    }

    @Bean
    @ConditionalOnMissingBean(com.jclinical.agenda.domain.ports.in.ManageRoomBlocksUseCase.class)
    public com.jclinical.agenda.domain.ports.in.ManageRoomBlocksUseCase manageRoomBlocksUseCase(
            com.jclinical.agenda.domain.ports.out.RoomBlockRepositoryPort roomBlockRepository,
            AppointmentRepositoryPort appointmentRepository,
            com.jclinical.agenda.domain.ports.out.RoomValidatorPort roomValidator,
            StaffPermissionCheckerPort permissionChecker) {
        return new com.jclinical.agenda.domain.service.RoomBlockService(
                roomBlockRepository, appointmentRepository, roomValidator, permissionChecker);
    }

    @Bean
    public MaterialReservationSchedulingService materialReservationSchedulingService(
            AppointmentRepositoryPort appointmentRepository,
            QuotationValidatorPort quotationValidator,
            ClinicSettingsPort clinicSettingsPort,
            DomainEventPublisherPort eventPublisher) {
        return new MaterialReservationSchedulingService(appointmentRepository, quotationValidator, clinicSettingsPort, eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean(com.jclinical.agenda.domain.ports.in.ManageWaitingListUseCase.class)
    public com.jclinical.agenda.domain.ports.in.ManageWaitingListUseCase manageWaitingListUseCase(
            com.jclinical.agenda.domain.ports.out.WaitingListRepositoryPort waitingListRepository,
            PatientValidatorPort patientValidator,
            StaffPermissionCheckerPort permissionChecker) {
        return new com.jclinical.agenda.domain.service.WaitingListService(waitingListRepository, patientValidator, permissionChecker);
    }

    @Bean
    @ConditionalOnMissingBean(AppointmentMapper.class)
    public AppointmentMapper appointmentMapper() {
        return new AppointmentMapper() {
            @Override
            public AppointmentEntity toEntity(Appointment domain) {
                if (domain == null) {
                    return null;
                }
                return AppointmentEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .patientId(domain.getPatientId())
                        .doctorStaffId(domain.getDoctorStaffId())
                        .roomId(domain.getRoomId())
                        .quotationId(domain.getQuotationId())
                        .quotationItemId(domain.getQuotationItemId())
                        .quotationItemIds(domain.getQuotationItemIds())
                        .serviceId(domain.getServiceId())
                        .serviceName(domain.getServiceName())
                        .servicePricing(domain.getServicePricing())
                        .servicePrice(domain.getServicePrice())
                        .seriesId(domain.getSeriesId())
                        .scheduledStart(domain.getScheduledStart())
                        .scheduledEnd(domain.getScheduledEnd())
                        .reason(domain.getReason())
                        .notes(domain.getNotes())
                        .cancellationReason(domain.getCancellationReason())
                        .cancelledAt(domain.getCancelledAt())
                        .cancelledByUserId(domain.getCancelledByUserId())
                        .status(domain.getStatus())
                        .materialsReserved(domain.isMaterialsReserved())
                        .externalCalendarEventId(domain.getExternalCalendarEventId())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public Appointment toDomain(AppointmentEntity entity) {
                if (entity == null) {
                    return null;
                }
                return Appointment.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .patientId(entity.getPatientId())
                        .doctorStaffId(entity.getDoctorStaffId())
                        .roomId(entity.getRoomId())
                        .quotationId(entity.getQuotationId())
                        .quotationItemId(entity.getQuotationItemId())
                        .quotationItemIds(entity.getQuotationItemIds())
                        .serviceId(entity.getServiceId())
                        .serviceName(entity.getServiceName())
                        .servicePricing(entity.getServicePricing())
                        .servicePrice(entity.getServicePrice())
                        .seriesId(entity.getSeriesId())
                        .scheduledStart(entity.getScheduledStart())
                        .scheduledEnd(entity.getScheduledEnd())
                        .reason(entity.getReason())
                        .notes(entity.getNotes())
                        .cancellationReason(entity.getCancellationReason())
                        .cancelledAt(entity.getCancelledAt())
                        .cancelledByUserId(entity.getCancelledByUserId())
                        .status(entity.getStatus())
                        .materialsReserved(entity.isMaterialsReserved())
                        .externalCalendarEventId(entity.getExternalCalendarEventId())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(ClinicScheduleMapper.class)
    public ClinicScheduleMapper clinicScheduleMapper() {
        return new ClinicScheduleMapper() {
            @Override
            public ClinicScheduleEntity toEntity(ClinicSchedule domain) {
                if (domain == null) {
                    return null;
                }
                return ClinicScheduleEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .dayOfWeek(domain.getDayOfWeek())
                        .open(domain.isOpen())
                        .startTime(domain.getStartTime())
                        .endTime(domain.getEndTime())
                        .build();
            }

            @Override
            public ClinicSchedule toDomain(ClinicScheduleEntity entity) {
                if (entity == null) {
                    return null;
                }
                return ClinicSchedule.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .dayOfWeek(entity.getDayOfWeek())
                        .open(entity.isOpen())
                        .startTime(entity.getStartTime())
                        .endTime(entity.getEndTime())
                        .build();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(RoomBlockMapper.class)
    public RoomBlockMapper roomBlockMapper() {
        return new RoomBlockMapper() {
            @Override
            public RoomBlockEntity toEntity(RoomBlock domain) {
                if (domain == null) return null;
                return RoomBlockEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .roomId(domain.getRoomId())
                        .startsAt(domain.getStartsAt())
                        .endsAt(domain.getEndsAt())
                        .type(domain.getType())
                        .reason(domain.getReason())
                        .createdByUserId(domain.getCreatedByUserId())
                        .active(domain.isActive())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public RoomBlock toDomain(RoomBlockEntity entity) {
                if (entity == null) return null;
                return RoomBlock.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .roomId(entity.getRoomId())
                        .startsAt(entity.getStartsAt())
                        .endsAt(entity.getEndsAt())
                        .type(entity.getType())
                        .reason(entity.getReason())
                        .createdByUserId(entity.getCreatedByUserId())
                        .active(entity.isActive())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }

            @Override
            public RoomBlockResponse toResponse(RoomBlock domain) {
                if (domain == null) return null;
                return new RoomBlockResponse(
                        domain.getId(), domain.getClinicId(), domain.getRoomId(), domain.getStartsAt(),
                        domain.getEndsAt(), domain.getType(), domain.getReason(), domain.getCreatedByUserId(),
                        domain.isActive(), domain.getCreatedAt(), domain.getUpdatedAt());
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(WaitingListMapper.class)
    public WaitingListMapper waitingListMapper() {
        return new WaitingListMapper() {
            @Override
            public WaitingListEntity toEntity(WaitingListEntry domain) {
                if (domain == null) return null;
                return WaitingListEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .patientId(domain.getPatientId())
                        .doctorStaffId(domain.getDoctorStaffId())
                        .roomId(domain.getRoomId())
                        .preferredDateFrom(domain.getPreferredDateFrom())
                        .preferredDateTo(domain.getPreferredDateTo())
                        .preferredTimeRange(domain.getPreferredTimeRange())
                        .notes(domain.getNotes())
                        .status(domain.getStatus())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public WaitingListEntry toDomain(WaitingListEntity entity) {
                if (entity == null) return null;
                return WaitingListEntry.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .patientId(entity.getPatientId())
                        .doctorStaffId(entity.getDoctorStaffId())
                        .roomId(entity.getRoomId())
                        .preferredDateFrom(entity.getPreferredDateFrom())
                        .preferredDateTo(entity.getPreferredDateTo())
                        .preferredTimeRange(entity.getPreferredTimeRange())
                        .notes(entity.getNotes())
                        .status(entity.getStatus())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }

            @Override
            public WaitingListResponse toResponse(WaitingListEntry domain) {
                if (domain == null) return null;
                return new WaitingListResponse(
                        domain.getId(), domain.getClinicId(), domain.getPatientId(), domain.getDoctorStaffId(),
                        domain.getRoomId(), domain.getPreferredDateFrom(), domain.getPreferredDateTo(),
                        domain.getPreferredTimeRange(), domain.getNotes(), domain.getStatus(),
                        domain.getCreatedAt(), domain.getUpdatedAt());
            }
        };
    }
}
