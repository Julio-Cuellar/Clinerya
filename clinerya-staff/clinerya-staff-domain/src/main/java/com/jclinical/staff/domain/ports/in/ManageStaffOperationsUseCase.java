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

    PayrollPeriodSummary createPayrollPeriod(UUID clinicId, String name, LocalDate periodStart, LocalDate periodEnd);

    List<PayrollPeriodSummary> listPayrollPeriods(UUID clinicId);

    PayrollLineSummary upsertPayrollLine(UUID clinicId, UUID periodId, UUID staffId, BigDecimal baseSalary,
                                         BigDecimal commissionAmount, BigDecimal bonusAmount,
                                         BigDecimal deductionAmount, String notes);

    PayrollPeriodSummary deletePayrollLine(UUID clinicId, UUID periodId, UUID staffId);

    List<PayrollLineSummary> listPayrollLines(UUID clinicId, UUID periodId);

    PayrollPeriodSummary closePayrollPeriod(UUID clinicId, UUID periodId);

    PayrollPeriodSummary payPayrollPeriod(UUID clinicId, UUID periodId, UUID bankAccountId, LocalDate paymentDate);

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
