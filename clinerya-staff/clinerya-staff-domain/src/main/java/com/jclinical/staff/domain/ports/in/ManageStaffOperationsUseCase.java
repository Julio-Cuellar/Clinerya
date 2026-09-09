package com.jclinical.staff.domain.ports.in;

import com.jclinical.staff.domain.model.StaffActivityType;
import com.jclinical.staff.domain.model.StaffAttendanceStatus;
import com.jclinical.staff.domain.model.StaffPayrollPeriodStatus;
import com.jclinical.staff.domain.model.StaffPayrollPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageStaffOperationsUseCase {
    AttendanceSummary clockIn(UUID clinicId, UUID staffId, LocalDateTime clockInAt, String notes);

    AttendanceSummary clockOut(UUID clinicId, UUID attendanceId, LocalDateTime clockOutAt, String notes);

    List<AttendanceSummary> listAttendance(UUID clinicId, UUID staffId, LocalDate from, LocalDate to);

    ActivitySummary recordActivity(UUID clinicId, UUID staffId, StaffActivityType type, String referenceType,
                                   UUID referenceId, String description, BigDecimal amount, LocalDateTime occurredAt);

    List<ActivitySummary> listActivities(UUID clinicId, UUID staffId, StaffActivityType type,
                                         LocalDateTime from, LocalDateTime to);

    PayrollPeriodSummary createPayrollPeriod(UUID clinicId, UUID actingUserId, String name, LocalDate periodStart,
                                            LocalDate periodEnd);

    List<PayrollPeriodSummary> listPayrollPeriods(UUID clinicId);

    PayrollLineSummary upsertPayrollLine(UUID clinicId, UUID actingUserId, UUID periodId, UUID staffId,
                                         BigDecimal baseSalary, BigDecimal commissionAmount, BigDecimal bonusAmount,
                                         BigDecimal deductionAmount, String notes);

    PayrollPeriodSummary deletePayrollLine(UUID clinicId, UUID actingUserId, UUID periodId, UUID staffId);

    List<PayrollLineSummary> listPayrollLines(UUID clinicId, UUID periodId);

    /**
     * Crea lineas para el personal activo que aun no tiene una en el periodo (no
     * sobrescribe las existentes). BASE_COMPENSATION usa el sueldo base de cada
     * empleado; PREVIOUS_PERIOD copia los montos del ultimo periodo cerrado.
     */
    List<PayrollLineSummary> generatePayrollLines(UUID clinicId, UUID actingUserId, UUID periodId, PayrollLineSource source);

    /**
     * Suma de la actividad registrada por cada empleado dentro del rango del
     * periodo, junto con la comision que ya tiene su linea (si existe).
     */
    List<CommissionPreviewEntry> previewPeriodCommissions(UUID clinicId, UUID actingUserId, UUID periodId);

    /**
     * Vuelca la suma de actividad del periodo en el campo comision de la linea de
     * cada empleado que tenga actividad y linea capturada. Recalcula totales.
     */
    List<PayrollLineSummary> applyPeriodCommissions(UUID clinicId, UUID actingUserId, UUID periodId);

    PayrollPeriodSummary closePayrollPeriod(UUID clinicId, UUID actingUserId, UUID periodId);

    PayrollPeriodSummary payPayrollPeriod(UUID clinicId, UUID actingUserId, UUID periodId, UUID bankAccountId,
                                          LocalDate paymentDate);

    enum PayrollLineSource {
        BASE_COMPENSATION,
        PREVIOUS_PERIOD
    }

    record CommissionPreviewEntry(
            UUID staffId,
            BigDecimal activityTotal,
            BigDecimal currentCommission
    ) {}

    record AttendanceSummary(
            UUID id,
            UUID clinicId,
            UUID staffId,
            LocalDate workDate,
            LocalDateTime clockInAt,
            LocalDateTime clockOutAt,
            StaffAttendanceStatus status,
            String notes
    ) {}

    record ActivitySummary(
            UUID id,
            UUID clinicId,
            UUID staffId,
            StaffActivityType type,
            String referenceType,
            UUID referenceId,
            String description,
            BigDecimal amount,
            LocalDateTime occurredAt
    ) {}

    record PayrollPeriodSummary(
            UUID id,
            UUID clinicId,
            String name,
            LocalDate periodStart,
            LocalDate periodEnd,
            StaffPayrollPeriodStatus status,
            StaffPayrollPaymentStatus paymentStatus,
            BigDecimal grossAmount,
            BigDecimal netAmount,
            LocalDateTime closedAt,
            LocalDateTime paidAt,
            UUID paymentAccountId,
            UUID paymentJournalEntryId
    ) {}

    record PayrollLineSummary(
            UUID id,
            UUID clinicId,
            UUID payrollPeriodId,
            UUID staffId,
            BigDecimal baseSalary,
            BigDecimal commissionAmount,
            BigDecimal bonusAmount,
            BigDecimal deductionAmount,
            BigDecimal grossAmount,
            BigDecimal netAmount,
            String notes
    ) {}
}
