package com.jclinical.treatments.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase.CreateCatalogItemCommand;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort;
import com.jclinical.treatments.domain.ports.out.TreatmentCatalogRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regresion de P3: antes, ClinicAccessInterceptor solo comprobaba pertenencia a la clinica,
 * asi que cualquier miembro activo podia dar de alta o modificar el catalogo de servicios.
 * Ahora se exige MANAGE_TREATMENT_CATALOG en el dominio.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TreatmentCatalogServiceAuthorizationTest {

    @Mock
    private TreatmentCatalogRepositoryPort catalogRepository;
    @Mock
    private InventoryMaterialPort inventoryMaterialPort;
    @Mock
    private StaffPermissionCheckerPort permissionChecker;

    @Mock
    private com.jclinical.treatments.domain.ports.out.ClinicSpecialtyPort clinicSpecialtyPort;

    private TreatmentCatalogService service;

    private UUID clinicId;
    private UUID actingUserId;

    @BeforeEach
    void setUp() {
        clinicId = UUID.randomUUID();
        actingUserId = UUID.randomUUID();
        service = new TreatmentCatalogService(
                catalogRepository, inventoryMaterialPort, permissionChecker, clinicSpecialtyPort);
    }

    @Test
    void deniesCreatingCatalogItemWithoutManagePermission() {
        CreateCatalogItemCommand command = new CreateCatalogItemCommand(
                "Limpieza", "PREVENTIVE", "desc", BigDecimal.TEN, 30, List.of());

        assertThrows(ClinicAccessDeniedException.class,
                () -> service.createCatalogItem(actingUserId, clinicId, command));

        verify(catalogRepository, never()).save(any());
    }

    @Test
    void deniesReadingCatalogWithoutViewPermission() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.getCatalogItemsByClinic(actingUserId, clinicId, false));
    }

    @Test
    void deniesEveryOperationWhenActingUserIsNull() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.getCatalogItemsByClinic(null, clinicId, false));
        verify(permissionChecker, never()).hasPermission(any(), any(), any());
    }

    @Test
    void allowsCreatingCatalogItemWhenPermissionGranted() {
        when(permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.MANAGE_TREATMENT_CATALOG))
                .thenReturn(true);
        when(catalogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCatalogItemCommand command = new CreateCatalogItemCommand(
                "Limpieza", "PREVENTIVE", "desc", BigDecimal.TEN, 30, List.of());

        service.createCatalogItem(actingUserId, clinicId, command);

        verify(catalogRepository).save(any());
    }
}
