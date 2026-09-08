package com.jclinical.staff.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.StaffAttendanceEntry;
import com.jclinical.staff.domain.model.StaffActivityLog;
import com.jclinical.staff.domain.model.StaffActivityType;
import com.jclinical.staff.domain.model.StaffCompensation;
import com.jclinical.staff.domain.model.StaffPayrollLine;
import com.jclinical.staff.domain.model.StaffPayrollPeriod;
import com.jclinical.staff.domain.model.StaffPayrollPeriodStatus;
import com.jclinical.staff.domain.model.StaffPermission;
import com.jclinical.staff.domain.ports.in.ManageStaffOperationsUseCase.PayrollLineSource;
import com.jclinical.staff.domain.ports.in.ManageStaffOperationsUseCase.PayrollLineSummary;
import com.jclinical.staff.domain.ports.in.ManageStaffOperationsUseCase.PayrollPeriodSummary;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.PayrollAccountingPort;
import com.jclinical.staff.domain.ports.out.StaffActivityRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffAttendanceRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffCompensationRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPayrollLineRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPayrollPeriodRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaffOperationsServiceTest {

    private final InMemoryClinicStaffRepo staffRepository = new InMemoryClinicStaffRepo();
    private final InMemoryPeriodRepo periodRepository = new InMemoryPeriodRepo();
    private final InMemoryLineRepo lineRepository = new InMemoryLineRepo();
    private final InMemoryCompRepo compensationRepository = new InMemoryCompRepo();
    private final Set<String> grants = new HashSet<>();

    private final UUID clinicId = UUID.randomUUID();
    private final UUID payrollUserId = UUID.randomUUID();
    private final UUID plainUserId = UUID.randomUUID();

    private StaffOperationsService service;

    @BeforeEach
    void setUp() {
        StaffPermissionCheckerPort permissionChecker = (clinic, userId, permission) ->
                grants.contains(userId + ":" + permission);
        PayrollAccountingPort accounting = (clinic, periodId, bankAccountId, gross, net, deduction, date) ->
                new PayrollAccountingPort.PaymentResult(UUID.randomUUID());

        service = new StaffOperationsService(
                staffRepository,
                new NoopAttendanceRepo(),
                new NoopActivityRepo(),
                periodRepository,
                lineRepository,
                accounting,
                permissionChecker,
                compensationRepository);

        grants.add(payrollUserId + ":" + StaffPermission.MANAGE_PAYROLL);
    }

    @Test
    void generateLinesFromBaseCompensationSkipsCapturedAndInactive() {
        UUID withSalary = staffRepository.seedActive(clinicId);
        UUID zeroSalary = staffRepository.seedActive(clinicId);
        UUID captured = staffRepository.seedActive(clinicId);
        UUID inactive = staffRepository.seedActive(clinicId);
        staffRepository.deactivate(inactive);
        compensationRepository.setBaseSalary(clinicId, withSalary, new BigDecimal("12000"));

        UUID periodId = service.createPayrollPeriod(
                clinicId, payrollUserId, "Nomina", LocalDate.now(), LocalDate.now().plusDays(14)).id();
        service.upsertPayrollLine(clinicId, payrollUserId, periodId, captured,
                new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "manual");

        List<PayrollLineSummary> lines = service.generatePayrollLines(
                clinicId, payrollUserId, periodId, PayrollLineSource.BASE_COMPENSATION);

        assertEquals(3, lines.size(), "withSalary + zeroSalary + captured; inactive queda fuera");
        assertEquals(0, new BigDecimal("12000").compareTo(lineFor(lines, withSalary).baseSalary()));
        assertEquals(0, BigDecimal.ZERO.compareTo(lineFor(lines, zeroSalary).baseSalary()));
        assertEquals(0, new BigDecimal("500").compareTo(lineFor(lines, captured).baseSalary()), "no sobrescribe");
    }

    @Test
    void generateLinesRequiresPermission() {
        UUID periodId = service.createPayrollPeriod(
                clinicId, payrollUserId, "Nomina", LocalDate.now(), LocalDate.now().plusDays(7)).id();
        assertThrows(ClinicAccessDeniedException.class, () ->
                service.generatePayrollLines(clinicId, plainUserId, periodId, PayrollLineSource.BASE_COMPENSATION));
    }

    @Test
    void generateLinesFromPreviousPeriodCopiesAmounts() {
        UUID staffId = staffRepository.seedActive(clinicId);

        UUID prevId = service.createPayrollPeriod(
                clinicId, payrollUserId, "Nomina 1", LocalDate.now().minusDays(20), LocalDate.now().minusDays(6)).id();
        service.upsertPayrollLine(clinicId, payrollUserId, prevId, staffId,
                new BigDecimal("10000"), new BigDecimal("1500"), new BigDecimal("300"), new BigDecimal("200"), "quincena");
        service.closePayrollPeriod(clinicId, payrollUserId, prevId);

        UUID currentId = service.createPayrollPeriod(
                clinicId, payrollUserId, "Nomina 2", LocalDate.now().minusDays(5), LocalDate.now().plusDays(9)).id();
        List<PayrollLineSummary> lines = service.generatePayrollLines(
                clinicId, payrollUserId, currentId, PayrollLineSource.PREVIOUS_PERIOD);

        assertEquals(1, lines.size());
        PayrollLineSummary copied = lineFor(lines, staffId);
        assertEquals(0, new BigDecimal("10000").compareTo(copied.baseSalary()));
        assertEquals(0, new BigDecimal("1500").compareTo(copied.commissionAmount()));
        assertEquals(0, new BigDecimal("11600").compareTo(copied.netAmount()));
    }

    @Test
    void generateFromPreviousPeriodFailsWhenNoneClosed() {
        UUID periodId = service.createPayrollPeriod(
                clinicId, payrollUserId, "Nomina", LocalDate.now(), LocalDate.now().plusDays(7)).id();
        assertThrows(IllegalStateException.class, () ->
                service.generatePayrollLines(clinicId, payrollUserId, periodId, PayrollLineSource.PREVIOUS_PERIOD));
    }

    private static PayrollLineSummary lineFor(List<PayrollLineSummary> lines, UUID staffId) {
        return lines.stream().filter(line -> line.staffId().equals(staffId)).findFirst().orElseThrow();
    }

    @Test
    void createPayrollPeriodRequiresManagePayrollPermission() {
        assertThrows(ClinicAccessDeniedException.class, () ->
                service.createPayrollPeriod(clinicId, plainUserId, "Nomina", LocalDate.now(), LocalDate.now()));
    }

    @Test
    void createPayrollPeriodRejectsAnonymousCaller() {
        assertThrows(ClinicAccessDeniedException.class, () ->
                service.createPayrollPeriod(clinicId, null, "Nomina", LocalDate.now(), LocalDate.now()));
    }

    @Test
    void createPayrollPeriodSucceedsWithPermission() {
        PayrollPeriodSummary period = service.createPayrollPeriod(
                clinicId, payrollUserId, "Nomina quincena 1", LocalDate.now(), LocalDate.now().plusDays(14));

        assertEquals("Nomina quincena 1", period.name());
        assertEquals(StaffPayrollPeriodStatus.DRAFT, period.status());
        assertEquals(1, periodRepository.store.size());
    }

    @Test
    void mutatingPayrollLinesRequiresPermission() {
        UUID staffId = staffRepository.seedActive(clinicId);
        UUID periodId = service.createPayrollPeriod(
                clinicId, payrollUserId, "Nomina", LocalDate.now(), LocalDate.now().plusDays(7)).id();

        assertThrows(ClinicAccessDeniedException.class, () -> service.upsertPayrollLine(
                clinicId, plainUserId, periodId, staffId,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null));
        assertThrows(ClinicAccessDeniedException.class, () ->
                service.deletePayrollLine(clinicId, plainUserId, periodId, staffId));
        assertThrows(ClinicAccessDeniedException.class, () ->
                service.closePayrollPeriod(clinicId, plainUserId, periodId));
        assertThrows(ClinicAccessDeniedException.class, () ->
                service.payPayrollPeriod(clinicId, plainUserId, periodId, UUID.randomUUID(), LocalDate.now()));
    }

    @Test
    void listingPayrollStaysReadableWithoutPermission() {
        service.createPayrollPeriod(clinicId, payrollUserId, "Nomina", LocalDate.now(), LocalDate.now().plusDays(7));

        // Reads are intentionally not gated in P0; the tab is hidden client-side instead.
        assertEquals(1, service.listPayrollPeriods(clinicId).size());
    }

    // --- in-memory fakes -------------------------------------------------

    private static final class InMemoryClinicStaffRepo implements ClinicStaffRepositoryPort {
        private final List<ClinicStaff> store = new ArrayList<>();

        UUID seedActive(UUID clinicId) {
            ClinicStaff staff = ClinicStaff.builder()
                    .id(UUID.randomUUID())
                    .clinicId(clinicId)
                    .userId(UUID.randomUUID())
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            store.add(staff);
            return staff.getId();
        }

        void deactivate(UUID staffId) {
            findById(staffId).ifPresent(staff -> staff.setActive(false));
        }

        @Override
        public ClinicStaff save(ClinicStaff staff) {
            store.removeIf(existing -> existing.getId().equals(staff.getId()));
            store.add(staff);
            return staff;
        }

        @Override
        public Optional<ClinicStaff> findById(UUID id) {
            return store.stream().filter(staff -> staff.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<ClinicStaff> findByClinicIdAndUserId(UUID clinicId, UUID userId) {
            return store.stream()
                    .filter(staff -> staff.getClinicId().equals(clinicId) && staff.getUserId().equals(userId))
                    .findFirst();
        }

        @Override
        public List<ClinicStaff> findByClinicId(UUID clinicId) {
            return store.stream().filter(staff -> staff.getClinicId().equals(clinicId)).toList();
        }

        @Override
        public List<ClinicStaff> findByUserId(UUID userId) {
            return store.stream().filter(staff -> staff.getUserId().equals(userId)).toList();
        }
    }

    private static final class InMemoryPeriodRepo implements StaffPayrollPeriodRepositoryPort {
        private final List<StaffPayrollPeriod> store = new ArrayList<>();

        @Override
        public StaffPayrollPeriod save(StaffPayrollPeriod period) {
            store.removeIf(existing -> existing.getId().equals(period.getId()));
            store.add(period);
            return period;
        }

        @Override
        public Optional<StaffPayrollPeriod> findById(UUID id) {
            return store.stream().filter(period -> period.getId().equals(id)).findFirst();
        }

        @Override
        public List<StaffPayrollPeriod> findByClinicId(UUID clinicId) {
            return store.stream().filter(period -> period.getClinicId().equals(clinicId)).toList();
        }
    }

    private static final class InMemoryLineRepo implements StaffPayrollLineRepositoryPort {
        private final List<StaffPayrollLine> store = new ArrayList<>();

        @Override
        public StaffPayrollLine save(StaffPayrollLine line) {
            store.removeIf(existing -> existing.getId().equals(line.getId()));
            store.add(line);
            return line;
        }

        @Override
        public void deleteById(UUID id) {
            store.removeIf(line -> line.getId().equals(id));
        }

        @Override
        public Optional<StaffPayrollLine> findByPeriodIdAndStaffId(UUID payrollPeriodId, UUID staffId) {
            return store.stream()
                    .filter(line -> line.getPayrollPeriodId().equals(payrollPeriodId) && line.getStaffId().equals(staffId))
                    .findFirst();
        }

        @Override
        public List<StaffPayrollLine> findByPeriodId(UUID payrollPeriodId) {
            return store.stream().filter(line -> line.getPayrollPeriodId().equals(payrollPeriodId)).toList();
        }
    }

    private static final class NoopAttendanceRepo implements StaffAttendanceRepositoryPort {
        @Override
        public StaffAttendanceEntry save(StaffAttendanceEntry entry) {
            return entry;
        }

        @Override
        public Optional<StaffAttendanceEntry> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<StaffAttendanceEntry> findOpenByStaffId(UUID clinicId, UUID staffId) {
            return Optional.empty();
        }

        @Override
        public List<StaffAttendanceEntry> findByClinicIdAndWorkDateBetween(UUID clinicId, LocalDate from, LocalDate to) {
            return List.of();
        }
    }

    private static final class NoopActivityRepo implements StaffActivityRepositoryPort {
        @Override
        public StaffActivityLog save(StaffActivityLog activity) {
            return activity;
        }

        @Override
        public List<StaffActivityLog> findByClinicIdAndOccurredAtBetween(UUID clinicId, LocalDateTime from, LocalDateTime to) {
            return List.of();
        }

        @Override
        public List<StaffActivityLog> findByClinicIdAndStaffIdAndOccurredAtBetween(UUID clinicId, UUID staffId,
                                                                                  LocalDateTime from, LocalDateTime to) {
            return List.of();
        }

        @Override
        public List<StaffActivityLog> findByClinicIdAndTypeAndOccurredAtBetween(UUID clinicId, StaffActivityType type,
                                                                               LocalDateTime from, LocalDateTime to) {
            return List.of();
        }
    }

    private static final class InMemoryCompRepo implements StaffCompensationRepositoryPort {
        private final List<StaffCompensation> store = new ArrayList<>();

        void setBaseSalary(UUID clinicId, UUID staffId, BigDecimal baseSalary) {
            store.removeIf(item -> item.getStaffId().equals(staffId));
            store.add(StaffCompensation.builder().staffId(staffId).clinicId(clinicId).baseSalary(baseSalary).build());
        }

        @Override
        public StaffCompensation save(StaffCompensation compensation) {
            store.removeIf(item -> item.getStaffId().equals(compensation.getStaffId()));
            store.add(compensation);
            return compensation;
        }

        @Override
        public Optional<StaffCompensation> findByStaffId(UUID staffId) {
            return store.stream().filter(item -> item.getStaffId().equals(staffId)).findFirst();
        }

        @Override
        public List<StaffCompensation> findByClinicId(UUID clinicId) {
            return store.stream().filter(item -> item.getClinicId().equals(clinicId)).toList();
        }
    }
}
