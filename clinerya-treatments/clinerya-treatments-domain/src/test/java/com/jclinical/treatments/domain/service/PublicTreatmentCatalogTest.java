package com.jclinical.treatments.domain.service;

import com.jclinical.treatments.domain.model.TreatmentCatalogItem;
import com.jclinical.treatments.domain.ports.in.PublicTreatmentCatalogUseCase.PublicTreatment;
import com.jclinical.treatments.domain.ports.out.TreatmentCatalogRepositoryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lo unico del catalogo que el asistente de WhatsApp puede decirle a un paciente: nombre, categoria y
 * precio de lista de los tratamientos activos. Sin costos internos ni materiales, y sin pedir un
 * usuario del personal (quien pregunta es el paciente, por una ruta interna).
 */
class PublicTreatmentCatalogTest {

    private final UUID clinicId = UUID.randomUUID();
    private final InMemoryCatalog catalog = new InMemoryCatalog();
    private final TreatmentCatalogService service = new TreatmentCatalogService(catalog, null,
            (clinic, user, permission) -> { throw new AssertionError("la ruta publica no pide permisos de personal"); }, null);

    @Test
    void onlyActiveTreatmentsWithNameCategoryAndListPriceSortedByName() {
        catalog.add("Resina", null, null, true);
        catalog.add("Limpieza dental", "Preventivo", new BigDecimal("650.00"), true);
        catalog.add("Blanqueamiento", "Estética", new BigDecimal("3500.00"), false);

        List<PublicTreatment> treatments = service.activeTreatments(clinicId);

        assertEquals(List.of(new PublicTreatment("Limpieza dental", "Preventivo", new BigDecimal("650.00")),
                new PublicTreatment("Resina", null, null)), treatments);
    }

    @Test
    void aClinicWithoutCatalogHasNothingToOffer() {
        assertEquals(List.of(), service.activeTreatments(clinicId));
    }

    final class InMemoryCatalog implements TreatmentCatalogRepositoryPort {
        final List<TreatmentCatalogItem> items = new ArrayList<>();

        void add(String name, String category, BigDecimal price, boolean active) {
            items.add(TreatmentCatalogItem.builder().id(UUID.randomUUID()).clinicId(clinicId).name(name).category(category)
                    .defaultPrice(price).active(active).build());
        }

        @Override public TreatmentCatalogItem save(TreatmentCatalogItem item) { items.add(item); return item; }

        @Override
        public Optional<TreatmentCatalogItem> findByIdAndClinicId(UUID itemId, UUID clinicId) {
            return items.stream().filter(item -> item.getId().equals(itemId)).findFirst();
        }

        @Override
        public List<TreatmentCatalogItem> findByClinicId(UUID clinicId, boolean includeInactive) {
            return items.stream().filter(item -> item.getClinicId().equals(clinicId) && (includeInactive || item.isActive())).toList();
        }
    }
}
