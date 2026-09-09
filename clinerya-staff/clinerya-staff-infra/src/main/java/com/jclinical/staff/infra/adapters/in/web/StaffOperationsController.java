package com.jclinical.staff.infra.adapters.in.web;

import com.jclinical.staff.domain.model.StaffActivityType;
import com.jclinical.staff.domain.ports.in.ManageStaffOperationsUseCase;
import com.jclinical.staff.domain.ports.in.ManageStaffOperationsUseCase.ActivitySummary;
import com.jclinical.staff.domain.ports.in.ManageStaffOperationsUseCase.AttendanceSummary;
import com.jclinical.staff.domain.ports.in.ManageStaffOperationsUseCase.PayrollLineSummary;
import com.jclinical.staff.domain.ports.in.ManageStaffOperationsUseCase.PayrollPeriodSummary;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffAttendanceRepositoryPort;
import com.jclinical.staff.domain.ports.out.UserDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/staff/operations")
@RequiredArgsConstructor
public class StaffOperationsController {

    private final ManageStaffOperationsUseCase operationsUseCase;
    private final ClinicStaffRepositoryPort clinicStaffRepository;
    private final StaffAttendanceRepositoryPort attendanceRepository;
    private final UserDirectoryPort userDirectory;

    @PostMapping("/attendance/clock-in")
    public ResponseEntity<AttendanceSummary> clockIn(
            @PathVariable UUID clinicId,
            @RequestBody ClockInRequest request,
            Principal principal) {
        ensureCurrentUserStaff(clinicId, request.staffId(), principal);
        AttendanceSummary summary = operationsUseCase.clockIn(clinicId, request.staffId(), request.clockInAt(), request.notes());
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @PostMapping("/attendance/{attendanceId}/clock-out")
    public ResponseEntity<AttendanceSummary> clockOut(
            @PathVariable UUID clinicId,
            @PathVariable UUID attendanceId,
            @RequestBody ClockOutRequest request,
            Principal principal) {
        var entry = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("Registro de asistencia no encontrado."));
        if (!entry.getClinicId().equals(clinicId)) {
            throw new IllegalArgumentException("El registro de asistencia no pertenece a esta clinica.");
        }
        ensureCurrentUserStaff(clinicId, entry.getStaffId(), principal);
        AttendanceSummary summary = operationsUseCase.clockOut(clinicId, attendanceId, request.clockOutAt(), request.notes());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/attendance")
    public ResponseEntity<List<AttendanceSummary>> listAttendance(
            @PathVariable UUID clinicId,
            @RequestParam(required = false) UUID staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(operationsUseCase.listAttendance(clinicId, staffId, from, to));
    }

    @PostMapping("/activities")
    public ResponseEntity<ActivitySummary> recordActivity(
            @PathVariable UUID clinicId,
            @RequestBody ActivityRequest request) {
        ActivitySummary summary = operationsUseCase.recordActivity(
                clinicId,
                request.staffId(),
                parseActivityType(request.type()),
                request.referenceType(),
                request.referenceId(),
                request.description(),
                request.amount(),
                request.occurredAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @GetMapping("/activities")
    public ResponseEntity<List<ActivitySummary>> listActivities(
            @PathVariable UUID clinicId,
            @RequestParam(required = false) UUID staffId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(operationsUseCase.listActivities(clinicId, staffId, parseActivityType(type), from, to));
    }

    @PostMapping("/payroll/periods")
    public ResponseEntity<PayrollPeriodSummary> createPayrollPeriod(
            @PathVariable UUID clinicId,
            @RequestBody PayrollPeriodRequest request,
            Principal principal) {
        PayrollPeriodSummary summary = operationsUseCase.createPayrollPeriod(
                clinicId,
                currentUserId(principal),
                request.name(),
                request.periodStart(),
                request.periodEnd());
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @GetMapping("/payroll/periods")
    public ResponseEntity<List<PayrollPeriodSummary>> listPayrollPeriods(@PathVariable UUID clinicId) {
        return ResponseEntity.ok(operationsUseCase.listPayrollPeriods(clinicId));
    }

    @PutMapping("/payroll/periods/{periodId}/lines/{staffId}")
    @Transactional
    public ResponseEntity<PayrollLineSummary> upsertPayrollLine(
            @PathVariable UUID clinicId,
            @PathVariable UUID periodId,
            @PathVariable UUID staffId,
            @RequestBody PayrollLineRequest request,
            Principal principal) {
        PayrollLineSummary summary = operationsUseCase.upsertPayrollLine(
                clinicId,
                currentUserId(principal),
                periodId,
                staffId,
                request.baseSalary(),
                request.commissionAmount(),
                request.bonusAmount(),
                request.deductionAmount(),
                request.notes());
        return ResponseEntity.ok(summary);
    }

    @DeleteMapping("/payroll/periods/{periodId}/lines/{staffId}")
    @Transactional
    public ResponseEntity<PayrollPeriodSummary> deletePayrollLine(
            @PathVariable UUID clinicId,
            @PathVariable UUID periodId,
            @PathVariable UUID staffId,
            Principal principal) {
        return ResponseEntity.ok(operationsUseCase.deletePayrollLine(clinicId, currentUserId(principal), periodId, staffId));
    }

    @GetMapping("/payroll/periods/{periodId}/lines")
    public ResponseEntity<List<PayrollLineSummary>> listPayrollLines(
            @PathVariable UUID clinicId,
            @PathVariable UUID periodId) {
        return ResponseEntity.ok(operationsUseCase.listPayrollLines(clinicId, periodId));
    }

    @PostMapping("/payroll/periods/{periodId}/lines/generate")
    @Transactional
    public ResponseEntity<List<PayrollLineSummary>> generatePayrollLines(
            @PathVariable UUID clinicId,
            @PathVariable UUID periodId,
            @RequestParam(required = false, defaultValue = "BASE_COMPENSATION") String source,
            Principal principal) {
        ManageStaffOperationsUseCase.PayrollLineSource parsedSource;
        try {
            parsedSource = ManageStaffOperationsUseCase.PayrollLineSource.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Origen de generacion no valido: " + source);
        }
        return ResponseEntity.ok(operationsUseCase.generatePayrollLines(
                clinicId, currentUserId(principal), periodId, parsedSource));
    }

    @GetMapping("/payroll/periods/{periodId}/commission-preview")
    public ResponseEntity<List<ManageStaffOperationsUseCase.CommissionPreviewEntry>> previewPeriodCommissions(
            @PathVariable UUID clinicId,
            @PathVariable UUID periodId,
            Principal principal) {
        return ResponseEntity.ok(operationsUseCase.previewPeriodCommissions(clinicId, currentUserId(principal), periodId));
    }

    @PostMapping("/payroll/periods/{periodId}/lines/apply-commissions")
    @Transactional
    public ResponseEntity<List<PayrollLineSummary>> applyPeriodCommissions(
            @PathVariable UUID clinicId,
            @PathVariable UUID periodId,
            Principal principal) {
        return ResponseEntity.ok(operationsUseCase.applyPeriodCommissions(clinicId, currentUserId(principal), periodId));
    }

    @PostMapping("/payroll/periods/{periodId}/close")
    public ResponseEntity<PayrollPeriodSummary> closePayrollPeriod(
            @PathVariable UUID clinicId,
            @PathVariable UUID periodId,
            Principal principal) {
        return ResponseEntity.ok(operationsUseCase.closePayrollPeriod(clinicId, currentUserId(principal), periodId));
    }

    @PostMapping("/payroll/periods/{periodId}/pay")
    @Transactional
    public ResponseEntity<PayrollPeriodSummary> payPayrollPeriod(
            @PathVariable UUID clinicId,
            @PathVariable UUID periodId,
            @RequestBody PayrollPaymentRequest request,
            Principal principal) {
        return ResponseEntity.ok(operationsUseCase.payPayrollPeriod(
                clinicId,
                currentUserId(principal),
                periodId,
                request.bankAccountId(),
                request.paymentDate()));
    }

    private StaffActivityType parseActivityType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        try {
            return StaffActivityType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Tipo de actividad no valido: " + type);
        }
    }

    private void ensureCurrentUserStaff(UUID clinicId, UUID staffId, Principal principal) {
        if (staffId == null) {
            throw new IllegalArgumentException("El empleado es obligatorio.");
        }
        UUID currentUserId = currentUserId(principal);
        boolean belongsToCurrentUser = clinicStaffRepository.findByClinicIdAndUserId(clinicId, currentUserId)
                .filter(staff -> staff.isActive() && staff.getId().equals(staffId))
                .isPresent();
        if (!belongsToCurrentUser) {
            throw new IllegalStateException("Solo puedes registrar tu propia entrada y salida.");
        }
    }

    private UUID currentUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalStateException("Usuario no autenticado.");
        }
        return userDirectory.findByEmail(principal.getName())
                .map(UserDirectoryPort.UserSummary::id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }

    public record ClockInRequest(
            UUID staffId,
            LocalDateTime clockInAt,
            String notes
    ) {}

    public record ClockOutRequest(
            LocalDateTime clockOutAt,
            String notes
    ) {}

    public record ActivityRequest(
            UUID staffId,
            String type,
            String referenceType,
            UUID referenceId,
            String description,
            BigDecimal amount,
            LocalDateTime occurredAt
    ) {}

    public record PayrollPeriodRequest(
            String name,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {}

    public record PayrollLineRequest(
            BigDecimal baseSalary,
            BigDecimal commissionAmount,
            BigDecimal bonusAmount,
            BigDecimal deductionAmount,
            String notes
    ) {}

    public record PayrollPaymentRequest(
            UUID bankAccountId,
            LocalDate paymentDate
    ) {}
}
