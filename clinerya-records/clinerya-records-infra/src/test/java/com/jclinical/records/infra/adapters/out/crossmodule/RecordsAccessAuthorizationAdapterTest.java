package com.jclinical.records.infra.adapters.out.crossmodule;

import com.jclinical.collaboration.domain.ports.in.ManageExternalAccessUseCase;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.PermissionItem;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.PermissionSummary;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.StaffSummary;
import com.jclinical.staff.domain.model.StaffPermissionOverrideState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Regresion de P3: este adaptador decidia el acceso al expediente solo por StaffRole, asi
 * que quitarle VIEW_MEDICAL_RECORDS a un doctor via override no le quitaba el acceso —
 * el rol seguia siendo DOCTOR. Ahora debe consultar getPermissions() (rol + overrides).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecordsAccessAuthorizationAdapterTest {

    @Mock
    private ManageClinicStaffUseCase clinicStaffUseCase;
    @Mock
    private ManageExternalAccessUseCase externalAccessUseCase;

    private RecordsAccessAuthorizationAdapter adapter;

    private UUID userId;
    private UUID clinicId;
    private UUID patientId;
    private UUID staffId;

    @BeforeEach
    void setUp() {
        adapter = new RecordsAccessAuthorizationAdapter(clinicStaffUseCase, externalAccessUseCase);
        userId = UUID.randomUUID();
        clinicId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        staffId = UUID.randomUUID();
        when(clinicStaffUseCase.getActiveStaffByUserAndClinic(userId, clinicId))
                .thenReturn(Optional.of(new StaffSummary(staffId, clinicId, userId, StaffRole.DOCTOR, "Doctora Demo")));
    }

    @Test
    void revokesAccessWhenAnOverrideDisablesViewMedicalRecordsEvenForADoctor() {
        when(clinicStaffUseCase.getPermissions(clinicId, staffId)).thenReturn(new PermissionSummary(
                staffId, StaffRole.DOCTOR, List.of(
                        new PermissionItem(StaffPermission.VIEW_MEDICAL_RECORDS, StaffPermissionOverrideState.REVOKED, false),
                        new PermissionItem(StaffPermission.EDIT_MEDICAL_RECORDS, StaffPermissionOverrideState.REVOKED, false)
                )));

        AccessDecision decision = adapter.resolveAccess(userId, clinicId, patientId);

        assertEquals(AccessLevel.NONE, decision.level());
    }

    @Test
    void grantsReadOnlyWhenOverrideAllowsViewButNotEdit() {
        when(clinicStaffUseCase.getPermissions(clinicId, staffId)).thenReturn(new PermissionSummary(
                staffId, StaffRole.DOCTOR, List.of(
                        new PermissionItem(StaffPermission.VIEW_MEDICAL_RECORDS, StaffPermissionOverrideState.INHERIT, true),
                        new PermissionItem(StaffPermission.EDIT_MEDICAL_RECORDS, StaffPermissionOverrideState.REVOKED, false)
                )));

        AccessDecision decision = adapter.resolveAccess(userId, clinicId, patientId);

        assertEquals(AccessLevel.READ_ONLY, decision.level());
    }

    @Test
    void grantsReadWriteByDefaultForADoctorWithNoOverrides() {
        when(clinicStaffUseCase.getPermissions(clinicId, staffId)).thenReturn(new PermissionSummary(
                staffId, StaffRole.DOCTOR, List.of(
                        new PermissionItem(StaffPermission.VIEW_MEDICAL_RECORDS, StaffPermissionOverrideState.INHERIT, true),
                        new PermissionItem(StaffPermission.EDIT_MEDICAL_RECORDS, StaffPermissionOverrideState.INHERIT, true)
                )));

        AccessDecision decision = adapter.resolveAccess(userId, clinicId, patientId);

        assertEquals(AccessLevel.READ_WRITE, decision.level());
    }
}
