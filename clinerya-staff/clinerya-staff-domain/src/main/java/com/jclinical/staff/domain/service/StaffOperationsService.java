package com.jclinical.staff.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.StaffCompensation;
import com.jclinical.staff.domain.model.StaffActivityLog;
import com.jclinical.staff.domain.model.StaffActivityType;
import com.jclinical.staff.domain.model.StaffAttendanceEntry;
import com.jclinical.staff.domain.model.StaffAttendanceStatus;
import com.jclinical.staff.domain.model.StaffPayrollLine;
import com.jclinical.staff.domain.model.StaffPayrollPeriod;
import com.jclinical.staff.domain.model.StaffPayrollPeriodStatus;
import com.jclinical.staff.domain.model.StaffPayrollPaymentStatus;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.staff.domain.ports.in.ManageStaffOperationsUseCase;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffActivityRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffAttendanceRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffCompensationRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPayrollLineRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPayrollPeriodRepositoryPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.staff.domain.ports.out.PayrollAccountingPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class StaffOperationsService implements ManageStaffOperationsUseCase {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ClinicStaffRepositoryPort clinicStaffRepository;
    private final StaffAttendanceRepositoryPort attendanceRepository;
    private final StaffActivityRepositoryPort activityRepository;
    private final StaffPayrollPeriodRepositoryPort payrollPeriodRepository;
    private final StaffPayrollLineRepositoryPort payrollLineRepository;
    private final PayrollAccountingPort payrollAccounting;
    private final StaffPermissionCheckerPort permissionChecker;
    private final StaffCompensationRepositoryPort staffCompensationRepository;

    public StaffOperationsService(ClinicStaffRepositoryPort clinicStaffRepository,
                                  StaffAttendanceRepositoryPort attendanceRepository,
                                  StaffActivityRepositoryPort activityRepository,
                                  StaffPayrollPeriodRepositoryPort payrollPeriodRepository,
                                  StaffPayrollLineRepositoryPort payrollLineRepository,
                                  PayrollAccountingPort payrollAccounting,
                                  StaffPermissionCheckerPort permissionChecker,
                                  StaffCompensationRepositoryPort staffCompensationRepository) {
        this.clinicStaffRepository = clinicStaffRepository;
        this.attendanceRepository = attendanceRepository;
        this.activityRepository = activityRepository;
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.payrollLineRepository = payrollLineRepository;
        this.payrollAccounting = payrollAccounting;
        this.permissionChecker = permissionChecker;
        this.staffCompensationRepository = staffCompensationRepository;
    }

    @Override
    public AttendanceSummary clockIn(UUID clinicId, UUID staffId, LocalDateTime clockInAt, String notes) {
        ensureActiveStaff(clinicId, staffId);
        if (attendanceRepository.findOpenByStaffId(clinicId, staffId).isPresent()) {
            throw new IllegalStateException("El empleado ya tiene una entrada abierta.");
        }
        LocalDateTime effectiveClockIn = clockInAt != null ? clockInAt : LocalDateTime.now();
        StaffAttendanceEntry entry = StaffAttendanceEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .staffId(staffId)
                .workDate(effectiveClockIn.toLocalDate())
                .clockInAt(effectiveClockIn)
                .status(StaffAttendanceStatus.OPEN)
                .notes(notes)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return toAttendanceSummary(attendanceRepository.save(entry));
    }

    @Override
    public AttendanceSummary clockOut(UUID clinicId, UUID attendanceId, LocalDateTime clockOutAt, String notes) {
        StaffAttendanceEntry entry = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("Registro de asistencia no encontrado."));
        if (!entry.getClinicId().equals(clinicId)) {
            throw new IllegalArgumentException("El registro de asistencia no pertenece a esta clinica.");
        }
        if (entry.getStatus() == StaffAttendanceStatus.CLOSED) {
            throw new IllegalStateException("El registro de asistencia ya esta cerrado.");
        }
        LocalDateTime effectiveClockOut = clockOutAt != null ? clockOutAt : LocalDateTime.now();
        if (effectiveClockOut.isBefore(entry.getClockInAt())) {
            throw new IllegalArgumentException("La hora de salida no puede ser anterior a la hora de entrada.");
        }
        entry.setClockOutAt(effectiveClockOut);
        entry.setStatus(StaffAttendanceStatus.CLOSED);
        entry.setNotes(mergeNotes(entry.getNotes(), notes));
        entry.setUpdatedAt(LocalDateTime.now());
        return toAttendanceSummary(attendanceRepository.save(entry));
    }

    @Override
    public List<AttendanceSummary> listAttendance(UUID clinicId, UUID staffId, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        return attendanceRepository.findByClinicIdAndWorkDateBetween(clinicId, effectiveFrom, effectiveTo).stream()
                .filter(entry -> staffId == null || entry.getStaffId().equals(staffId))
                .map(this::toAttendanceSummary)
                .toList();
    }

    @Override
    public ActivitySummary recordActivity(UUID clinicId, UUID staffId, StaffActivityType type, String referenceType,
                                          UUID referenceId, String description, BigDecimal amount, LocalDateTime occurredAt) {
        ensureActiveStaff(clinicId, staffId);
        if (type == null) {
            throw new IllegalArgumentException("El tipo de actividad es obligatorio.");
        }
        StaffActivityLog activity = StaffActivityLog.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .staffId(staffId)
                .type(type)
                .referenceType(blankToNull(referenceType))
                .referenceId(referenceId)
                .description(blankToNull(description))
                .amount(amount != null ? amount : ZERO)
                .occurredAt(occurredAt != null ? occurredAt : LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        return toActivitySummary(activityRepository.save(activity));
    }

    @Override
    public List<ActivitySummary> listActivities(UUID clinicId, UUID staffId, StaffActivityType type,
                                                LocalDateTime from, LocalDateTime to) {
        LocalDateTime effectiveFrom = from != null ? from : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();
        List<StaffActivityLog> activities;
        if (staffId != null) {
            activities = activityRepository.findByClinicIdAndStaffIdAndOccurredAtBetween(clinicId, staffId, effectiveFrom, effectiveTo);
        } else if (type != null) {
            activities = activityRepository.findByClinicIdAndTypeAndOccurredAtBetween(clinicId, type, effectiveFrom, effectiveTo);
        } else {
            activities = activityRepository.findByClinicIdAndOccurredAtBetween(clinicId, effectiveFrom, effectiveTo);
        }
        return activities.stream()
                .filter(activity -> type == null || activity.getType() == type)
                .map(this::toActivitySummary)
                .toList();
    }

    @Override
    public PayrollPeriodSummary createPayrollPeriod(UUID clinicId, UUID actingUserId, String name, LocalDate periodStart,
                                                   LocalDate periodEnd) {
        requirePayrollPermission(clinicId, actingUserId);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del periodo de nomina es obligatorio.");
        }
        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("El rango del periodo de nomina no es valido.");
        }
        StaffPayrollPeriod period = StaffPayrollPeriod.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .name(name.trim())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .status(StaffPayrollPeriodStatus.DRAFT)
                .paymentStatus(StaffPayrollPaymentStatus.UNPAID)
                .grossAmount(ZERO)
                .netAmount(ZERO)
                .createdAt(LocalDateTime.now())
                .build();
        return toPayrollPeriodSummary(payrollPeriodRepository.save(period));
    }

    @Override
    public List<PayrollPeriodSummary> listPayrollPeriods(UUID clinicId) {
        return payrollPeriodRepository.findByClinicId(clinicId).stream()
                .map(this::toPayrollPeriodSummary)
                .toList();
    }

    @Override
    public PayrollLineSummary upsertPayrollLine(UUID clinicId, UUID actingUserId, UUID periodId, UUID staffId,
                                                BigDecimal baseSalary, BigDecimal commissionAmount, BigDecimal bonusAmount,
                                                BigDecimal deductionAmount, String notes) {
        requirePayrollPermission(clinicId, actingUserId);
        ensureActiveStaff(clinicId, staffId);
        StaffPayrollPeriod period = findPayrollPeriod(clinicId, periodId);
        ensureDraft(period);
        StaffPayrollLine line = payrollLineRepository.findByPeriodIdAndStaffId(periodId, staffId)
                .orElseGet(() -> StaffPayrollLine.builder()
                        .id(UUID.randomUUID())
                        .clinicId(clinicId)
                        .payrollPeriodId(periodId)
                        .staffId(staffId)
                        .createdAt(LocalDateTime.now())
                        .build());
        line.setBaseSalary(amountOrZero(baseSalary));
        line.setCommissionAmount(amountOrZero(commissionAmount));
        line.setBonusAmount(amountOrZero(bonusAmount));
        line.setDeductionAmount(amountOrZero(deductionAmount));
        line.setGrossAmount(line.getBaseSalary().add(line.getCommissionAmount()).add(line.getBonusAmount()));
        line.setNetAmount(line.getGrossAmount().subtract(line.getDeductionAmount()));
        line.setNotes(blankToNull(notes));
        line.setUpdatedAt(LocalDateTime.now());
        StaffPayrollLine saved = payrollLineRepository.save(line);
        refreshPayrollTotals(period);
        return toPayrollLineSummary(saved);
    }

    @Override
    public PayrollPeriodSummary deletePayrollLine(UUID clinicId, UUID actingUserId, UUID periodId, UUID staffId) {
        requirePayrollPermission(clinicId, actingUserId);
        StaffPayrollPeriod period = findPayrollPeriod(clinicId, periodId);
        ensureDraft(period);
        StaffPayrollLine line = payrollLineRepository.findByPeriodIdAndStaffId(periodId, staffId)
                .orElseThrow(() -> new IllegalArgumentException("La linea de nomina no existe en este periodo."));
        payrollLineRepository.deleteById(line.getId());
        refreshPayrollTotals(period);
        return toPayrollPeriodSummary(period);
    }

    @Override
    public List<PayrollLineSummary> listPayrollLines(UUID clinicId, UUID periodId) {
        findPayrollPeriod(clinicId, periodId);
        return payrollLineRepository.findByPeriodId(periodId).stream()
                .map(this::toPayrollLineSummary)
                .toList();
    }

    @Override
    public List<PayrollLineSummary> generatePayrollLines(UUID clinicId, UUID actingUserId, UUID periodId,
                                                         PayrollLineSource source) {
        requirePayrollPermission(clinicId, actingUserId);
        StaffPayrollPeriod period = findPayrollPeriod(clinicId, periodId);
        ensureDraft(period);
        PayrollLineSource effectiveSource = source != null ? source : PayrollLineSource.BASE_COMPENSATION;

        Set<UUID> alreadyCaptured = payrollLineRepository.findByPeriodId(periodId).stream()
                .map(StaffPayrollLine::getStaffId)
                .collect(Collectors.toSet());

        if (effectiveSource == PayrollLineSource.PREVIOUS_PERIOD) {
            StaffPayrollPeriod previous = payrollPeriodRepository.findByClinicId(clinicId).stream()
                    .filter(candidate -> !candidate.getId().equals(periodId))
                    .filter(candidate -> candidate.getStatus() == StaffPayrollPeriodStatus.CLOSED)
                    .max(Comparator.comparing(StaffPayrollPeriod::getPeriodEnd)
                            .thenComparing(candidate -> candidate.getCreatedAt() != null
                                    ? candidate.getCreatedAt() : LocalDateTime.MIN))
                    .orElseThrow(() -> new IllegalStateException("No hay un periodo de nomina cerrado para copiar."));
            for (StaffPayrollLine template : payrollLineRepository.findByPeriodId(previous.getId())) {
                if (alreadyCaptured.contains(template.getStaffId()) || !isActiveStaff(clinicId, template.getStaffId())) {
                    continue;
                }
                saveGeneratedLine(clinicId, periodId, template.getStaffId(), template.getBaseSalary(),
                        template.getCommissionAmount(), template.getBonusAmount(), template.getDeductionAmount(),
                        template.getNotes());
            }
        } else {
            for (ClinicStaff member : clinicStaffRepository.findByClinicId(clinicId)) {
                if (!member.isActive() || alreadyCaptured.contains(member.getId())) {
                    continue;
                }
                BigDecimal base = staffCompensationRepository.findByStaffId(member.getId())
                        .map(StaffCompensation::getBaseSalary)
                        .orElse(ZERO);
                saveGeneratedLine(clinicId, periodId, member.getId(), base, ZERO, ZERO, ZERO, null);
            }
        }

        refreshPayrollTotals(period);
        return payrollLineRepository.findByPeriodId(periodId).stream()
                .map(this::toPayrollLineSummary)
                .toList();
    }

    @Override
    public List<CommissionPreviewEntry> previewPeriodCommissions(UUID clinicId, UUID actingUserId, UUID periodId) {
        requirePayrollPermission(clinicId, actingUserId);
        StaffPayrollPeriod period = findPayrollPeriod(clinicId, periodId);
        Map<UUID, BigDecimal> activityByStaff = activityTotalsForPeriod(clinicId, period);
        Map<UUID, BigDecimal> commissionByStaff = payrollLineRepository.findByPeriodId(periodId).stream()
                .collect(Collectors.toMap(StaffPayrollLine::getStaffId,
                        line -> amountOrZero(line.getCommissionAmount()), (a, b) -> a));
        return activityByStaff.entrySet().stream()
                .map(entry -> new CommissionPreviewEntry(entry.getKey(), entry.getValue(),
                        commissionByStaff.getOrDefault(entry.getKey(), ZERO)))
                .toList();
    }

    @Override
    public List<PayrollLineSummary> applyPeriodCommissions(UUID clinicId, UUID actingUserId, UUID periodId) {
        requirePayrollPermission(clinicId, actingUserId);
        StaffPayrollPeriod period = findPayrollPeriod(clinicId, periodId);
        ensureDraft(period);
        Map<UUID, BigDecimal> activityByStaff = activityTotalsForPeriod(clinicId, period);

        for (StaffPayrollLine line : payrollLineRepository.findByPeriodId(periodId)) {
            BigDecimal activityTotal = activityByStaff.get(line.getStaffId());
            if (activityTotal == null || activityTotal.signum() <= 0) {
                continue;
            }
            line.setCommissionAmount(activityTotal);
            line.setGrossAmount(amountOrZero(line.getBaseSalary())
                    .add(activityTotal).add(amountOrZero(line.getBonusAmount())));
            line.setNetAmount(line.getGrossAmount().subtract(amountOrZero(line.getDeductionAmount())));
            line.setUpdatedAt(LocalDateTime.now());
            payrollLineRepository.save(line);
        }

        refreshPayrollTotals(period);
        return payrollLineRepository.findByPeriodId(periodId).stream()
                .map(this::toPayrollLineSummary)
                .toList();
    }

    private Map<UUID, BigDecimal> activityTotalsForPeriod(UUID clinicId, StaffPayrollPeriod period) {
        LocalDateTime from = period.getPeriodStart().atStartOfDay();
        LocalDateTime to = period.getPeriodEnd().atTime(LocalTime.MAX);
        Map<UUID, BigDecimal> totals = new HashMap<>();
        for (StaffActivityLog activity : activityRepository.findByClinicIdAndOccurredAtBetween(clinicId, from, to)) {
            totals.merge(activity.getStaffId(), amountOrZero(activity.getAmount()), BigDecimal::add);
        }
        return totals;
    }

    private void saveGeneratedLine(UUID clinicId, UUID periodId, UUID staffId, BigDecimal base, BigDecimal commission,
                                   BigDecimal bonus, BigDecimal deduction, String notes) {
        StaffPayrollLine line = StaffPayrollLine.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .payrollPeriodId(periodId)
                .staffId(staffId)
                .createdAt(LocalDateTime.now())
                .build();
        line.setBaseSalary(amountOrZero(base));
        line.setCommissionAmount(amountOrZero(commission));
        line.setBonusAmount(amountOrZero(bonus));
        line.setDeductionAmount(amountOrZero(deduction));
        line.setGrossAmount(line.getBaseSalary().add(line.getCommissionAmount()).add(line.getBonusAmount()));
        line.setNetAmount(line.getGrossAmount().subtract(line.getDeductionAmount()));
        line.setNotes(blankToNull(notes));
        line.setUpdatedAt(LocalDateTime.now());
        payrollLineRepository.save(line);
    }

    private boolean isActiveStaff(UUID clinicId, UUID staffId) {
        return clinicStaffRepository.findById(staffId)
                .filter(staff -> staff.getClinicId().equals(clinicId) && staff.isActive())
                .isPresent();
    }

    @Override
    public PayrollPeriodSummary closePayrollPeriod(UUID clinicId, UUID actingUserId, UUID periodId) {
        requirePayrollPermission(clinicId, actingUserId);
        StaffPayrollPeriod period = findPayrollPeriod(clinicId, periodId);
        ensureDraft(period);
        refreshPayrollTotals(period);
        period.setStatus(StaffPayrollPeriodStatus.CLOSED);
        period.setClosedAt(LocalDateTime.now());
        return toPayrollPeriodSummary(payrollPeriodRepository.save(period));
    }

    @Override
    public PayrollPeriodSummary payPayrollPeriod(UUID clinicId, UUID actingUserId, UUID periodId, UUID bankAccountId,
                                                LocalDate paymentDate) {
        requirePayrollPermission(clinicId, actingUserId);
        StaffPayrollPeriod period = findPayrollPeriod(clinicId, periodId);
        if (period.getStatus() != StaffPayrollPeriodStatus.CLOSED) {
            throw new IllegalStateException("Cierra el periodo antes de generar el pago.");
        }
        if (period.getPaymentStatus() == StaffPayrollPaymentStatus.PAID) {
            throw new IllegalStateException("El periodo de nomina ya fue pagado.");
        }
        if (bankAccountId == null) {
            throw new IllegalArgumentException("Selecciona la cuenta desde la que se realizara el pago.");
        }
        BigDecimal grossAmount = amountOrZero(period.getGrossAmount());
        BigDecimal netAmount = amountOrZero(period.getNetAmount());
        BigDecimal deductionAmount = grossAmount.subtract(netAmount);
        if (grossAmount.signum() <= 0 || netAmount.signum() <= 0 || deductionAmount.signum() < 0) {
            throw new IllegalStateException("El periodo no tiene un importe neto valido para pagar.");
        }

        PayrollAccountingPort.PaymentResult payment = payrollAccounting.registerPayrollPayment(
                clinicId,
                periodId,
                bankAccountId,
                grossAmount,
                netAmount,
                deductionAmount,
                paymentDate != null ? paymentDate : LocalDate.now());
        period.setPaymentStatus(StaffPayrollPaymentStatus.PAID);
        period.setPaidAt(LocalDateTime.now());
        period.setPaymentAccountId(bankAccountId);
        period.setPaymentJournalEntryId(payment.journalEntryId());
        return toPayrollPeriodSummary(payrollPeriodRepository.save(period));
    }

    private void requirePayrollPermission(UUID clinicId, UUID actingUserId) {
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("Usuario no autenticado.");
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.MANAGE_PAYROLL)) {
            throw new ClinicAccessDeniedException("No tienes permiso para gestionar la nomina de esta clinica.");
        }
    }

    private ClinicStaff ensureActiveStaff(UUID clinicId, UUID staffId) {
        ClinicStaff staff = clinicStaffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Miembro del personal no encontrado."));
        if (!staff.getClinicId().equals(clinicId) || !staff.isActive()) {
            throw new IllegalArgumentException("El miembro del personal no esta activo en esta clinica.");
        }
        return staff;
    }

    private StaffPayrollPeriod findPayrollPeriod(UUID clinicId, UUID periodId) {
        StaffPayrollPeriod period = payrollPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Periodo de nomina no encontrado."));
        if (!period.getClinicId().equals(clinicId)) {
            throw new IllegalArgumentException("El periodo de nomina no pertenece a esta clinica.");
        }
        return period;
    }

    private void ensureDraft(StaffPayrollPeriod period) {
        if (period.getStatus() != StaffPayrollPeriodStatus.DRAFT) {
            throw new IllegalStateException("El periodo de nomina ya esta cerrado.");
        }
    }

    private void refreshPayrollTotals(StaffPayrollPeriod period) {
        if (period.getPaymentStatus() == null) {
            period.setPaymentStatus(StaffPayrollPaymentStatus.UNPAID);
        }
        BigDecimal gross = ZERO;
        BigDecimal net = ZERO;
        for (StaffPayrollLine line : payrollLineRepository.findByPeriodId(period.getId())) {
            gross = gross.add(amountOrZero(line.getGrossAmount()));
            net = net.add(amountOrZero(line.getNetAmount()));
        }
        period.setGrossAmount(gross);
        period.setNetAmount(net);
        payrollPeriodRepository.save(period);
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : ZERO;
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String mergeNotes(String current, String extra) {
        if (extra == null || extra.trim().isEmpty()) {
            return current;
        }
        if (current == null || current.trim().isEmpty()) {
            return extra.trim();
        }
        return current + "\n" + extra.trim();
    }

    private AttendanceSummary toAttendanceSummary(StaffAttendanceEntry entry) {
        return new AttendanceSummary(entry.getId(), entry.getClinicId(), entry.getStaffId(), entry.getWorkDate(),
                entry.getClockInAt(), entry.getClockOutAt(), entry.getStatus(), entry.getNotes());
    }

    private ActivitySummary toActivitySummary(StaffActivityLog activity) {
        return new ActivitySummary(activity.getId(), activity.getClinicId(), activity.getStaffId(), activity.getType(),
                activity.getReferenceType(), activity.getReferenceId(), activity.getDescription(), activity.getAmount(),
                activity.getOccurredAt());
    }

    private PayrollPeriodSummary toPayrollPeriodSummary(StaffPayrollPeriod period) {
        return new PayrollPeriodSummary(period.getId(), period.getClinicId(), period.getName(), period.getPeriodStart(),
                period.getPeriodEnd(), period.getStatus(), period.getPaymentStatus() != null ? period.getPaymentStatus() : StaffPayrollPaymentStatus.UNPAID,
                period.getGrossAmount(), period.getNetAmount(),
                period.getClosedAt(), period.getPaidAt(), period.getPaymentAccountId(), period.getPaymentJournalEntryId());
    }

    private PayrollLineSummary toPayrollLineSummary(StaffPayrollLine line) {
        return new PayrollLineSummary(line.getId(), line.getClinicId(), line.getPayrollPeriodId(), line.getStaffId(),
                line.getBaseSalary(), line.getCommissionAmount(), line.getBonusAmount(), line.getDeductionAmount(),
                line.getGrossAmount(), line.getNetAmount(), line.getNotes());
    }
}
