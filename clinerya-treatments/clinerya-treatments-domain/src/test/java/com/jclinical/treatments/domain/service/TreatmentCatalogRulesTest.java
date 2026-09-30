package com.jclinical.treatments.domain.service;

import com.jclinical.treatments.domain.model.PricingType;
import com.jclinical.treatments.domain.model.TreatmentCatalogItem;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase.CreateCatalogItemCommand;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase.UpdateCatalogItemCommand;
import com.jclinical.treatments.domain.ports.in.PublicTreatmentCatalogUseCase.PublicService;
import com.jclinical.treatments.domain.ports.out.TreatmentCatalogRepositoryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reglas del catalogo de servicios (plan v2, S1): tipo de precio (fijo o varia por paciente), duracion
 * obligatoria y "disponible en el asistente", que exige una descripcion breve para el paciente. El
 * asistente solo ve servicios disponibles y completos; la agenda lee un servicio por su id.
 */
class TreatmentCatalogRulesTest {

    private static final String DESCRIPTION = "Retiramos sarro y placa con ultrasonido y pulimos tus dientes.";

    private final UUID clinicId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Catalog catalog = new Catalog();
    private final TreatmentCatalogService service =
            new TreatmentCatalogService(catalog, null, (clinic, user, permission) -> true, null);

    @Test
    void aFixedPriceServiceKeepsItsTypePriceDurationAndAssistantFlag() {
        TreatmentCatalogItem saved = service.createCatalogItem(userId, clinicId, create("Limpieza dental",
                PricingType.FIXED, new BigDecimal("650"), 45, true, DESCRIPTION));

        assertEquals(PricingType.FIXED, saved.getPricingType());
        assertEquals(45, saved.getEstimatedDurationMinutes());
        assertTrue(saved.isAvailableInAssistant());
    }

    @Test
    void aFixedPriceNeedsAPriceButAVariablePriceDoesNot() {
        assertThrows(IllegalArgumentException.class, () -> service.createCatalogItem(userId, clinicId,
                create("Limpieza", PricingType.FIXED, null, 45, false, null)));

        TreatmentCatalogItem variable = service.createCatalogItem(userId, clinicId,
                create("Ortodoncia", PricingType.VARIES_BY_PATIENT, null, 60, false, null));
        assertEquals(PricingType.VARIES_BY_PATIENT, variable.getPricingType());
    }

    @Test
    void thePricingTypeIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> service.createCatalogItem(userId, clinicId,
                create("Limpieza", null, new BigDecimal("650"), 45, false, null)));
    }

    @Test
    void theDurationIsRequiredAndPositive() {
        assertThrows(IllegalArgumentException.class, () -> service.createCatalogItem(userId, clinicId,
                create("Limpieza", PricingType.FIXED, new BigDecimal("650"), null, false, null)));
        assertThrows(IllegalArgumentException.class, () -> service.createCatalogItem(userId, clinicId,
                create("Limpieza", PricingType.FIXED, new BigDecimal("650"), 0, false, null)));
    }

    @Test
    void aServiceOfferedByTheAssistantNeedsABriefDescription() {
        assertThrows(IllegalArgumentException.class, () -> service.createCatalogItem(userId, clinicId,
                create("Limpieza", PricingType.FIXED, new BigDecimal("650"), 45, true, null)));
        assertThrows(IllegalArgumentException.class, () -> service.createCatalogItem(userId, clinicId,
                create("Limpieza", PricingType.FIXED, new BigDecimal("650"), 45, true, "Muy corta")));
        assertThrows(IllegalArgumentException.class, () -> service.createCatalogItem(userId, clinicId,
                create("Limpieza", PricingType.FIXED, new BigDecimal("650"), 45, true, "x".repeat(301))));
    }

    @Test
    void updatingAppliesTheSameRules() {
        TreatmentCatalogItem saved = service.createCatalogItem(userId, clinicId,
                create("Blanqueamiento", PricingType.FIXED, new BigDecimal("3200"), 60, false, null));

        assertThrows(IllegalArgumentException.class, () -> service.updateCatalogItem(userId, saved.getId(), clinicId,
                update("Blanqueamiento", PricingType.FIXED, new BigDecimal("3200"), 60, true, null)));
        TreatmentCatalogItem updated = service.updateCatalogItem(userId, saved.getId(), clinicId,
                update("Blanqueamiento", PricingType.VARIES_BY_PATIENT, new BigDecimal("2800"), 60, true, DESCRIPTION));
        assertEquals(PricingType.VARIES_BY_PATIENT, updated.getPricingType());
        assertTrue(updated.isAvailableInAssistant());
    }

    @Test
    void theAssistantOnlySeesActiveAvailableAndCompleteServices() {
        catalog.put(item("Limpieza dental", PricingType.FIXED, new BigDecimal("650"), 45, true, DESCRIPTION, true));
        catalog.put(item("Ortodoncia", PricingType.VARIES_BY_PATIENT, new BigDecimal("18000"), 60, true, DESCRIPTION, true));
        catalog.put(item("Extracción", PricingType.FIXED, new BigDecimal("900"), 40, false, DESCRIPTION, true));
        catalog.put(item("Blanqueamiento", PricingType.FIXED, new BigDecimal("3200"), null, true, null, true));
        catalog.put(item("Resina", PricingType.FIXED, new BigDecimal("1200"), 30, true, DESCRIPTION, false));

        List<PublicService> services = service.assistantServices(clinicId);

        assertEquals(List.of("Limpieza dental", "Ortodoncia"), services.stream().map(PublicService::name).toList());
        PublicService limpieza = services.getFirst();
        assertEquals(DESCRIPTION, limpieza.description());
        assertEquals(PricingType.FIXED, limpieza.pricingType());
        assertEquals(45, limpieza.durationMinutes());
        assertEquals(new BigDecimal("650"), limpieza.price());
    }

    @Test
    void theAgendaReadsAnActiveServiceByItsId() {
        TreatmentCatalogItem limpieza = catalog.put(item("Limpieza dental", PricingType.FIXED, new BigDecimal("650"), 45,
                false, null, true));
        TreatmentCatalogItem inactive = catalog.put(item("Resina", PricingType.FIXED, new BigDecimal("1200"), 30,
                false, null, false));

        Optional<PublicService> found = service.activeService(clinicId, limpieza.getId());

        assertEquals(Optional.of(45), found.map(PublicService::durationMinutes));
        assertFalse(service.activeService(clinicId, inactive.getId()).isPresent());
        assertFalse(service.activeService(UUID.randomUUID(), limpieza.getId()).isPresent(), "otra clinica no la ve");
    }

    private static CreateCatalogItemCommand create(String name, PricingType type, BigDecimal price, Integer duration,
                                                   boolean assistant, String description) {
        return new CreateCatalogItemCommand(name, "General", description, price, duration, List.of(), type, assistant);
    }

    private static UpdateCatalogItemCommand update(String name, PricingType type, BigDecimal price, Integer duration,
                                                   boolean assistant, String description) {
        return new UpdateCatalogItemCommand(name, "General", description, price, duration, List.of(), true, type, assistant);
    }

    private TreatmentCatalogItem item(String name, PricingType type, BigDecimal price, Integer duration, boolean assistant,
                                      String description, boolean active) {
        return TreatmentCatalogItem.builder().id(UUID.randomUUID()).clinicId(clinicId).name(name).category("General")
                .description(description).defaultPrice(price).estimatedDurationMinutes(duration).pricingType(type)
                .availableInAssistant(assistant).active(active).build();
    }

    static final class Catalog implements TreatmentCatalogRepositoryPort {
        final List<TreatmentCatalogItem> items = new ArrayList<>();

        TreatmentCatalogItem put(TreatmentCatalogItem item) {
            items.add(item);
            return item;
        }

        @Override
        public TreatmentCatalogItem save(TreatmentCatalogItem item) {
            items.removeIf(existing -> existing.getId().equals(item.getId()));
            items.add(item);
            return item;
        }

        @Override
        public Optional<TreatmentCatalogItem> findByIdAndClinicId(UUID itemId, UUID clinicId) {
            return items.stream().filter(item -> item.getId().equals(itemId) && item.getClinicId().equals(clinicId)).findFirst();
        }

        @Override
        public List<TreatmentCatalogItem> findByClinicId(UUID clinicId, boolean includeInactive) {
            return items.stream().filter(item -> item.getClinicId().equals(clinicId) && (includeInactive || item.isActive()))
                    .toList();
        }
    }
}
