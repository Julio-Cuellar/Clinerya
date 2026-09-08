package com.jclinical.staff.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.StaffCompensation;
import com.jclinical.staff.domain.model.StaffPayFrequency;
import com.jclinical.staff.domain.model.StaffPaymentMethod;
import com.jclinical.staff.domain.model.StaffPermission;
import com.jclinical.staff.domain.ports.in.ManageStaffCompensationUseCase.CompensationInput;
import com.jclinical.staff.domain.ports.in.ManageStaffCompensationUseCase.CompensationSummary;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffCompensationRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

class StaffCompensationServiceTest {

    private final InMemoryCompensationRepo compensationRepo = new InMemoryCompensationRepo();
    private final InMemoryStaffRepo staffRepo = new InMemoryStaffRepo();
    private final Set<String> grants = new HashSet<>();

    private final UUID clinicId = UUID.randomUUID();
    private final UUID payrollUserId = UUID.randomUUID();
    private final UUID plainUserId = UUID.randomUUID();

    private StaffCompensationService service;
    private UUID staffId;

    @BeforeEach
    void setUp() {
        StaffPermissionCheckerPort checker = (clinic, userId, permission) -> grants.contains(userId + ":" + permission);
        service = new StaffCompensationService(compensationRepo, staffRepo, checker);
        grants.add(payrollUserId + ":" + StaffPermission.MANAGE_PAYROLL);
        staffId = staffRepo.seedActive(clinicId);
    }

    private CompensationInput sampleInput() {
        return new CompensationInput(new BigDecimal("12500.00"), StaffPayFrequency.MONTHLY,
                StaffPaymentMethod.BANK_TRANSFER, "002010077777777771", "AAAA010101AAA", null, null);
    }

    @Test
    void getReturnsDefaultsWhenNoRecord() {
        CompensationSummary summary = service.getCompensation(clinicId, staffId);
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.baseSalary()));
        assertEquals(StaffPayFrequency.BIWEEKLY, summary.payFrequency());
        assertEquals(StaffPaymentMethod.BANK_TRANSFER, summary.paymentMethod());
        assertNull(summary.rfc());
    }

    @Test
    void updateRequiresManagePayrollPermission() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.updateCompensation(clinicId, plainUserId, staffId, sampleInput()));
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.updateCompensation(clinicId, null, staffId, sampleInput()));
    }

    @Test
    void updateUpsertsAndListsByClinic() {
        CompensationSummary saved = service.updateCompensation(clinicId, payrollUserId, staffId, sampleInput());
        assertEquals(0, new BigDecimal("12500.00").compareTo(saved.baseSalary()));
        assertEquals(StaffPayFrequency.MONTHLY, saved.payFrequency());
        assertEquals("AAAA010101AAA", saved.rfc());

        service.updateCompensation(clinicId, payrollUserId, staffId,
                new CompensationInput(new BigDecimal("9000"), StaffPayFrequency.WEEKLY, StaffPaymentMethod.CASH,
                        "  ", "", null, null));

        List<CompensationSummary> all = service.listByClinic(clinicId);
        assertEquals(1, all.size());
        assertEquals(0, new BigDecimal("9000").compareTo(all.get(0).baseSalary()));
        assertEquals(StaffPaymentMethod.CASH, all.get(0).paymentMethod());
        assertNull(all.get(0).paymentAccountClabe());
    }

    @Test
    void updateRejectsNegativeSalary() {
        assertThrows(IllegalArgumentException.class, () -> service.updateCompensation(clinicId, payrollUserId, staffId,
                new CompensationInput(new BigDecimal("-1"), null, null, null, null, null, null)));
    }

    @Test
    void rejectsStaffFromAnotherClinic() {
        UUID otherStaff = staffRepo.seedActive(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> service.getCompensation(clinicId, otherStaff));
    }

    private static final class InMemoryCompensationRepo implements StaffCompensationRepositoryPort {
        private final List<StaffCompensation> store = new ArrayList<>();

        @Override
        public StaffCompensation save(StaffCompensation compensation) {
            store.removeIf(existing -> existing.getStaffId().equals(compensation.getStaffId()));
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

    private static final class InMemoryStaffRepo implements ClinicStaffRepositoryPort {
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
}
