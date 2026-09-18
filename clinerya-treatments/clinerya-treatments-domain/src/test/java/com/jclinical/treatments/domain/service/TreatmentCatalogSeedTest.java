package com.jclinical.treatments.domain.service;

import com.jclinical.core.domain.ClinicSpecialty;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.treatments.domain.model.TreatmentCatalogItem;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase.SeedResult;
import com.jclinical.treatments.domain.ports.out.ClinicSpecialtyPort;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * El onboarding es salteable y reanudable, así que sembrar dos veces va a pasar. La siembra tiene
 * que ser idempotente o la clínica termina con el catálogo duplicado.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TreatmentCatalogSeedTest {

    @Mock
    private TreatmentCatalogRepositoryPort catalogRepository;

    @Mock
    private InventoryMaterialPort inventoryMaterialPort;

    @Mock
    private StaffPermissionCheckerPort permissionChecker;

    @Mock
    private ClinicSpecialtyPort clinicSpecialtyPort;

    private TreatmentCatalogService service;

    private UUID clinicId;
    private UUID actingUserId;
    private List<TreatmentCatalogItem> stored;

    @BeforeEach
    void setUp() {
        service = new TreatmentCatalogService(
                catalogRepository, inventoryMaterialPort, permissionChecker, clinicSpecialtyPort);
        clinicId = UUID.randomUUID();
        actingUserId = UUID.randomUUID();
        stored = new ArrayList<>();

        when(permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.MANAGE_TREATMENT_CATALOG))
                .thenReturn(true);
        when(catalogRepository.findByClinicId(eq(clinicId), anyBoolean())).thenAnswer(call -> List.copyOf(stored));
        when(catalogRepository.save(any(TreatmentCatalogItem.class))).thenAnswer(call -> {
            TreatmentCatalogItem item = call.getArgument(0);
            stored.add(item);
            return item;
        });
    }

    private void givenSpecialty(ClinicSpecialty specialty) {
        when(clinicSpecialtyPort.findByClinicId(clinicId)).thenReturn(Optional.of(specialty));
    }

    @Test
    void seedsTheDentalCatalog() {
        givenSpecialty(ClinicSpecialty.ODONTOLOGIA);

        SeedResult result = service.seedCatalogForSpecialty(actingUserId, clinicId);

        assertEquals(18, result.created());
        assertEquals(0, result.skipped());
        assertEquals(18, stored.size());
        assertTrue(stored.stream().allMatch(item -> item.getClinicId().equals(clinicId)));
        // Sin inventario que enganchar en una clinica recien creada.
        assertTrue(stored.stream().allMatch(item -> item.getMaterials().isEmpty()));
    }

    @Test
    void seedingTwiceDoesNotDuplicate() {
        givenSpecialty(ClinicSpecialty.ODONTOLOGIA);

        SeedResult first = service.seedCatalogForSpecialty(actingUserId, clinicId);
        SeedResult second = service.seedCatalogForSpecialty(actingUserId, clinicId);

        assertEquals(18, first.created());
        assertEquals(0, second.created());
        assertEquals(18, second.skipped());
        assertEquals(18, stored.size());
    }

    @Test
    void keepsServicesTheClinicAlreadyCreatedByHand() {
        givenSpecialty(ClinicSpecialty.NUTRICION);
        stored.add(TreatmentCatalogItem.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                // Mismo servicio con otra caja y espacios: sigue siendo el mismo para la clinica.
                .name("  consulta de SEGUIMIENTO ")
                .defaultPrice(BigDecimal.valueOf(750))
                .active(true)
                .build());

        SeedResult result = service.seedCatalogForSpecialty(actingUserId, clinicId);

        assertEquals(1, result.skipped());
        assertTrue(stored.stream()
                .filter(item -> item.getName().trim().equalsIgnoreCase("consulta de seguimiento"))
                .allMatch(item -> item.getDefaultPrice().compareTo(BigDecimal.valueOf(750)) == 0));
    }

    @Test
    void unconfiguredClinicHasNothingToSeed() {
        givenSpecialty(ClinicSpecialty.SIN_CONFIGURAR);

        assertThrows(IllegalArgumentException.class,
                () -> service.seedCatalogForSpecialty(actingUserId, clinicId));
        assertTrue(stored.isEmpty());
    }

    @Test
    void requiresCatalogPermission() {
        givenSpecialty(ClinicSpecialty.ODONTOLOGIA);
        when(permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.MANAGE_TREATMENT_CATALOG))
                .thenReturn(false);

        assertThrows(ClinicAccessDeniedException.class,
                () -> service.seedCatalogForSpecialty(actingUserId, clinicId));
        assertTrue(stored.isEmpty());
    }
}
