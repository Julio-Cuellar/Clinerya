package com.jclinical.staff.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.staff.domain.model.StaffCompensation;
import com.jclinical.staff.domain.model.StaffInvitationCompensation;
import com.jclinical.staff.domain.model.StaffPayFrequency;
import com.jclinical.staff.domain.model.StaffPaymentMethod;
import com.jclinical.staff.domain.model.StaffPermission;
import com.jclinical.staff.domain.ports.in.ManageStaffCompensationUseCase.CompensationInput;
import com.jclinical.staff.domain.ports.out.StaffCompensationRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffInvitationCompensationRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffOnboardingServiceTest {

    private final InMemoryInvitationCompRepo invitationCompRepo = new InMemoryInvitationCompRepo();
    private final InMemoryCompRepo compRepo = new InMemoryCompRepo();
    private final Set<String> grants = new HashSet<>();

    private final UUID clinicId = UUID.randomUUID();
    private final UUID payrollUser = UUID.randomUUID();
    private final UUID plainUser = UUID.randomUUID();

    private StaffOnboardingService service;

    @BeforeEach
    void setUp() {
        StaffPermissionCheckerPort checker = (clinic, userId, permission) -> grants.contains(userId + ":" + permission);
        service = new StaffOnboardingService(checker, invitationCompRepo, compRepo);
        grants.add(payrollUser + ":" + StaffPermission.MANAGE_PAYROLL);
    }

    private CompensationInput input() {
        return new CompensationInput(new BigDecimal("15000"), StaffPayFrequency.MONTHLY,
                StaffPaymentMethod.BANK_TRANSFER, "002010077777777771", "AAAA010101AAA", null, null);
    }

    @Test
    void setInvitationCompensationRequiresPayrollPermission() {
        UUID invitationId = UUID.randomUUID();
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.setInvitationCompensation(clinicId, plainUser, invitationId, input()));
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.setInvitationCompensation(clinicId, null, invitationId, input()));
        assertTrue(invitationCompRepo.store.isEmpty());
    }

    @Test
    void setInvitationCompensationStoresPendingRow() {
        UUID invitationId = UUID.randomUUID();
        service.setInvitationCompensation(clinicId, payrollUser, invitationId, input());
        StaffInvitationCompensation pending = invitationCompRepo.store.get(0);
        assertEquals(invitationId, pending.getInvitationId());
        assertEquals(0, new BigDecimal("15000").compareTo(pending.getBaseSalary()));
        assertEquals(StaffPayFrequency.MONTHLY, pending.getPayFrequency());
    }

    @Test
    void setInvitationCompensationWithNullClearsPendingRow() {
        UUID invitationId = UUID.randomUUID();
        service.setInvitationCompensation(clinicId, payrollUser, invitationId, input());
        service.setInvitationCompensation(clinicId, payrollUser, invitationId, null);
        assertTrue(invitationCompRepo.store.isEmpty());
    }

    @Test
    void applyInvitationCompensationCopiesThenClears() {
        UUID invitationId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        invitationCompRepo.save(StaffInvitationCompensation.builder()
                .invitationId(invitationId).clinicId(clinicId)
                .baseSalary(new BigDecimal("9000")).payFrequency(StaffPayFrequency.WEEKLY)
                .paymentMethod(StaffPaymentMethod.CASH).build());

        service.applyInvitationCompensation(invitationId, staffId, clinicId);

        StaffCompensation saved = compRepo.findByStaffId(staffId).orElseThrow();
        assertEquals(0, new BigDecimal("9000").compareTo(saved.getBaseSalary()));
        assertEquals(StaffPaymentMethod.CASH, saved.getPaymentMethod());
        assertTrue(invitationCompRepo.findByInvitationId(invitationId).isEmpty());
    }

    @Test
    void applyInvitationCompensationIsNoOpWhenNothingPending() {
        service.applyInvitationCompensation(UUID.randomUUID(), UUID.randomUUID(), clinicId);
        assertTrue(compRepo.store.isEmpty());
    }

    // --- fakes -------------------------------------------------------------

    private static final class InMemoryInvitationCompRepo implements StaffInvitationCompensationRepositoryPort {
        private final List<StaffInvitationCompensation> store = new ArrayList<>();

        @Override
        public StaffInvitationCompensation save(StaffInvitationCompensation compensation) {
            store.removeIf(item -> item.getInvitationId().equals(compensation.getInvitationId()));
            store.add(compensation);
            return compensation;
        }

        @Override
        public Optional<StaffInvitationCompensation> findByInvitationId(UUID invitationId) {
            return store.stream().filter(item -> item.getInvitationId().equals(invitationId)).findFirst();
        }

        @Override
        public void deleteByInvitationId(UUID invitationId) {
            store.removeIf(item -> item.getInvitationId().equals(invitationId));
        }
    }

    private static final class InMemoryCompRepo implements StaffCompensationRepositoryPort {
        private final List<StaffCompensation> store = new ArrayList<>();

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
