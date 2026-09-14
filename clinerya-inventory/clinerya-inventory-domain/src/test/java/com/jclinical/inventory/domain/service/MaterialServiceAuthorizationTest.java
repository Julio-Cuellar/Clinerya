package com.jclinical.inventory.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.inventory.domain.model.Material;
import com.jclinical.inventory.domain.ports.in.ManageMaterialUseCase.CreateMaterialCommand;
import com.jclinical.inventory.domain.ports.out.MaterialRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresion de P3: ClinicAccessInterceptor solo comprobaba pertenencia a la clinica,
 * asi que cualquier miembro activo podia dar de alta o consultar materiales del
 * inventario. Ahora se exige MANAGE_MATERIALS/VIEW_INVENTORY en el dominio.
 */
class MaterialServiceAuthorizationTest {

    private final InMemoryMaterialRepository materialRepository = new InMemoryMaterialRepository();
    private final UUID clinicId = UUID.randomUUID();
    private final UUID actingUserId = UUID.randomUUID();
    private StaffPermission grantedPermission;
    private final StaffPermissionCheckerPort permissionChecker =
            (clinic, user, permission) -> permission == grantedPermission;

    private MaterialService service;

    @BeforeEach
    void setUp() {
        service = new MaterialService(materialRepository, permissionChecker);
    }

    private CreateMaterialCommand createCommand() {
        return new CreateMaterialCommand(
                "Guantes", "Desechables", "COD-1", "Marca", "desc", "par", "Caja",
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, false, null, false);
    }

    @Test
    void deniesCreatingMaterialWithoutManagePermission() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.createMaterial(actingUserId, clinicId, createCommand()));
        assertTrue(materialRepository.items.isEmpty());
    }

    @Test
    void deniesReadingMaterialsWithoutViewPermission() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.getMaterialsByClinic(actingUserId, clinicId, false));
    }

    @Test
    void deniesEveryOperationWhenActingUserIsNull() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.getMaterialsByClinic(null, clinicId, false));
    }

    @Test
    void systemLookupBypassesPermissionCheck() {
        UUID materialId = UUID.randomUUID();
        materialRepository.items.add(Material.builder().id(materialId).clinicId(clinicId).active(true).build());

        Optional<Material> found = service.getMaterial(materialId, clinicId);

        assertTrue(found.isPresent());
    }

    @Test
    void allowsCreatingMaterialWhenPermissionGranted() {
        grantedPermission = StaffPermission.MANAGE_MATERIALS;

        Material created = service.createMaterial(actingUserId, clinicId, createCommand());

        assertEquals(1, materialRepository.items.size());
        assertEquals(created.getId(), materialRepository.items.get(0).getId());
    }

    private static final class InMemoryMaterialRepository implements MaterialRepositoryPort {
        private final List<Material> items = new ArrayList<>();

        @Override
        public Material save(Material material) {
            items.removeIf(existing -> existing.getId().equals(material.getId()));
            items.add(material);
            return material;
        }

        @Override
        public Optional<Material> findByIdAndClinicId(UUID materialId, UUID clinicId) {
            return items.stream()
                    .filter(item -> item.getId().equals(materialId) && item.getClinicId().equals(clinicId))
                    .findFirst();
        }

        @Override
        public Optional<Material> findByIdAndClinicIdForUpdate(UUID materialId, UUID clinicId) {
            return findByIdAndClinicId(materialId, clinicId);
        }

        @Override
        public List<Material> findByClinicId(UUID clinicId, boolean includeInactive) {
            return items.stream()
                    .filter(item -> item.getClinicId().equals(clinicId))
                    .filter(item -> includeInactive || item.isActive())
                    .toList();
        }
    }
}
