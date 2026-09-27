package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.ports.out.AssistantProfilePort;
import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort;
import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort.CatalogTreatment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicios de la clinica desde el catalogo de tratamientos. El precio de lista solo sale si la
 * clinica lo permite; si no hay coincidencia con lo que se busco, se muestra el catalogo para ofrecer
 * alternativas en vez de decir que no se encontro.
 */
public final class ServicesTool implements AgentTool {

    public static final String NAME = "servicios_y_precios";
    static final String PRICES_SHOWN = "Son precios desde; el precio final lo define el médico en la valoración.";
    static final String PRICES_HIDDEN = "Los precios no se comparten por chat; se revisan en la valoración.";
    private static final int MAX_SERVICES = 25;

    private final TreatmentCatalogPort catalog;
    private final AssistantProfilePort profiles;

    public ServicesTool(TreatmentCatalogPort catalog, AssistantProfilePort profiles) {
        this.catalog = catalog;
        this.profiles = profiles;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Servicios y tratamientos que ofrece la clínica, con precio de lista solo si la clínica "
                + "lo permite. Usa busqueda para un tratamiento en particular; sin busqueda devuelve todos.",
                List.of(new ToolSpec.Parameter("busqueda", "string", "Tratamiento que busca el paciente, opcional.", false)));
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        AssistantProfile profile = profiles.find(context.clinicId());
        boolean showPrices = profile != null && profile.showPrices();
        List<CatalogTreatment> all = catalog.activeTreatments(context.clinicId());
        String query = ToolArgs.normalize(ToolArgs.text(arguments, "busqueda"));
        List<CatalogTreatment> matched = query.isEmpty() ? all : all.stream().filter(item -> matches(item, query)).toList();
        boolean match = query.isEmpty() || !matched.isEmpty();

        List<Map<String, Object>> services = new ArrayList<>();
        List<String> facts = new ArrayList<>();
        for (CatalogTreatment item : (match ? matched : all).stream().limit(MAX_SERVICES).toList()) {
            Map<String, Object> service = new LinkedHashMap<>();
            service.put("nombre", item.name());
            facts.add(item.name());
            if (item.category() != null && !item.category().isBlank()) {
                service.put("categoria", item.category());
            }
            if (showPrices && item.price() != null) {
                String price = ToolArgs.price(item.price());
                service.put("precio_desde", price);
                facts.add(price);
            }
            services.add(Map.copyOf(service));
        }
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("servicios", List.copyOf(services));
        content.put("coincidencia", match);
        content.put("nota_precios", showPrices ? PRICES_SHOWN : PRICES_HIDDEN);
        return new ToolOutcome(content, List.of(), facts);
    }

    private static boolean matches(CatalogTreatment item, String query) {
        String name = ToolArgs.normalize(item.name());
        String category = ToolArgs.normalize(item.category());
        return name.contains(query) || (!category.isEmpty() && category.contains(query)) || query.contains(name);
    }
}
