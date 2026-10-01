package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort.CatalogTreatment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "¿Cuanto cuesta la consulta general?": el precio sale del catalogo de tratamientos aunque el
 * paciente lo diga con otras palabras, en otro orden o en plural. Si hay varios parecidos se le
 * pregunta cual; si no hay precio se cotiza en la valoracion; nunca se inventa una cifra.
 */
class ServicesPricingTest {

    private final UUID clinicId = UUID.randomUUID();
    private final List<CatalogTreatment> catalog = List.of(
            new CatalogTreatment("Consulta general odontológica", "Consultas", new BigDecimal("500")),
            new CatalogTreatment("Consulta de ortodoncia", "Consultas", new BigDecimal("700")),
            new CatalogTreatment("Limpieza dental", "Preventivo", new BigDecimal("650")),
            new CatalogTreatment("Blanqueamiento", "Estética", null));
    private final ServicesTool tool = new ServicesTool(clinic -> catalog, clinic -> new AssistantProfile(null, null, true));

    @Test
    void aFreelyWordedQuestionFindsTheTreatmentAndItsListPrice() {
        ToolOutcome outcome = tool.run(context(), Map.of("busqueda", "¿Cuánto cuesta la consulta general?"));

        assertEquals(List.of("Consulta general odontológica"), names(outcome));
        assertEquals("$500", services(outcome).getFirst().get("precio"), "un precio fijo es lo que cuesta");
        assertTrue(outcome.facts().contains("$500"));
        assertTrue(outcome.options().isEmpty(), "con una sola coincidencia no se pregunta cual");
    }

    @Test
    void wordOrderAndPluralsDoNotMatter() {
        assertEquals(List.of("Consulta general odontológica"), names(tool.run(context(), Map.of("busqueda", "general consulta"))));
        assertEquals(List.of("Limpieza dental"), names(tool.run(context(), Map.of("busqueda", "limpiezas dentales"))));
    }

    @Test
    void severalSimilarTreatmentsAreOfferedToChooseFrom() {
        ToolOutcome outcome = tool.run(context(), Map.of("busqueda", "consulta"));

        assertEquals(List.of("Consulta de ortodoncia", "Consulta general odontológica"), names(outcome).stream().sorted().toList());
        assertEquals(List.of("Consulta de ortodoncia", "Consulta general odontológica"),
                outcome.options().stream().map(ConversationOption::label).sorted().toList());
        assertEquals(true, outcome.content().get("varias_opciones"));
        assertTrue(outcome.fallback() != null && outcome.fallback().contains("servicios"), String.valueOf(outcome.fallback()));
    }

    @Test
    void aTreatmentWithoutPriceIsQuotedAtTheAssessment() {
        ToolOutcome outcome = tool.run(context(), Map.of("busqueda", "blanqueamiento"));

        Map<String, Object> service = services(outcome).getFirst();
        assertFalse(service.containsKey("precio_desde"));
        assertEquals(ServicesTool.QUOTED_AT_ASSESSMENT, service.get("precio"));
    }

    @Test
    void afterGivingAPriceTheAgentOffersToBook() {
        ToolOutcome outcome = tool.run(context(), Map.of("busqueda", "limpieza"));

        assertEquals(ServicesTool.OFFER_TO_BOOK, outcome.content().get("siguiente_paso"));
    }

    @Test
    void aQuestionWithoutATreatmentShowsTheWholeCatalog() {
        ToolOutcome outcome = tool.run(context(), Map.of("busqueda", "¿cuánto cuesta?"));

        assertEquals(4, names(outcome).size());
        assertEquals(true, outcome.content().get("coincidencia"));
    }

    @Test
    void pricesAreSharedByDefaultWhenTheClinicHasNotDecided() {
        assertTrue(AssistantProfile.EMPTY.showPrices(), "compartir precios viene encendido");
    }

    private ToolContext context() {
        return new ToolContext(clinicId, UUID.randomUUID(), "5215512345678", List.of(), List.of(),
                LocalDateTime.of(2026, 9, 30, 10, 0));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> services(ToolOutcome outcome) {
        return (List<Map<String, Object>>) outcome.content().get("servicios");
    }

    private static List<Object> names(ToolOutcome outcome) {
        return services(outcome).stream().map(service -> service.get("nombre")).toList();
    }
}
