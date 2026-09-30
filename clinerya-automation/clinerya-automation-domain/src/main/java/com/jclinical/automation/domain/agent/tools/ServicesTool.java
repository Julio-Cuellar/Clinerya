package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.ports.out.AssistantProfilePort;
import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort;
import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort.CatalogTreatment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servicios y precios de la clinica desde el catalogo de tratamientos:
 * <ul>
 *   <li>La busqueda es por palabras (sin acentos, en cualquier orden, singular o plural): "¿cuanto
 *       cuesta la consulta general?" encuentra "Consulta general odontologica".</li>
 *   <li>Gana el tratamiento con mas palabras en comun; si varios empatan se ofrecen para elegir.</li>
 *   <li>El precio es "desde" y solo si la clinica lo comparte; sin precio en el catalogo se cotiza en
 *       la valoracion. Ninguna cifra sale de otro lado.</li>
 *   <li>Sin coincidencia se muestra el catalogo para ofrecer alternativas.</li>
 * </ul>
 */
public final class ServicesTool implements AgentTool {

    public static final String NAME = "servicios_y_precios";
    public static final String OPTION_PREFIX = "servicio:";
    public static final String QUOTED_AT_ASSESSMENT = "Se cotiza en la valoración.";
    public static final String OFFER_TO_BOOK = "Ofrece agendarle una cita para ese servicio.";
    static final String PRICES_SHOWN = "precio es lo que cuesta (precio fijo). precio_desde es solo referencia: el precio "
            + "exacto lo define el médico en la valoración.";
    static final String PRICES_HIDDEN = "Los precios no se comparten por chat; se revisan en la valoración.";
    private static final int MAX_SERVICES = 25;
    /** Limite de filas de una lista de WhatsApp. */
    private static final int MAX_OPTIONS = 10;
    private static final int MIN_WORD = 2;
    private static final int MIN_PREFIX = 4;
    /** Palabras de la pregunta que no dicen que tratamiento se busca. */
    private static final Set<String> FILLER = Set.of("cuanto", "cuesta", "cuestan", "costo", "costos", "precio", "precios",
            "vale", "valen", "cobran", "sale", "tiene", "tienen", "hacen", "quiero", "quisiera", "saber", "informacion",
            "una", "uno", "unos", "unas", "el", "la", "los", "las", "de", "del", "al", "para", "por", "con", "en", "que", "me",
            "mi", "su", "sus", "y", "o", "es", "son", "hay", "tratamiento", "tratamientos", "servicio", "servicios");

    private final TreatmentCatalogPort catalog;
    private final AssistantProfilePort profiles;

    public ServicesTool(TreatmentCatalogPort catalog, AssistantProfilePort profiles) {
        this.catalog = catalog;
        this.profiles = profiles;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Servicios y tratamientos que ofrece la clínica, con precio de lista si la clínica lo "
                + "comparte. Úsala siempre que pregunten por un servicio o su costo; pasa en busqueda lo que dijo el paciente. "
                + "Sin busqueda devuelve todos.",
                List.of(new ToolSpec.Parameter("busqueda", "string",
                        "Servicio o pregunta del paciente, por ejemplo: consulta general, limpieza, cuánto cuesta un blanqueamiento.",
                        false)));
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        AssistantProfile profile = profiles.find(context.clinicId());
        boolean showPrices = profile == null ? AssistantProfile.EMPTY.showPrices() : profile.showPrices();
        List<CatalogTreatment> all = catalog.activeTreatments(context.clinicId());
        List<String> wanted = words(ToolArgs.text(arguments, "busqueda"));

        List<CatalogTreatment> matched = wanted.isEmpty() ? all : best(all, wanted);
        boolean match = wanted.isEmpty() || !matched.isEmpty();
        List<CatalogTreatment> shown = (match ? matched : all).stream().limit(MAX_SERVICES).toList();
        boolean several = !wanted.isEmpty() && matched.size() > 1;

        List<Map<String, Object>> services = new ArrayList<>();
        List<String> facts = new ArrayList<>();
        for (CatalogTreatment item : shown) {
            services.add(describe(item, showPrices, facts));
        }
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("servicios", List.copyOf(services));
        content.put("coincidencia", match);
        content.put("nota_precios", showPrices ? PRICES_SHOWN : PRICES_HIDDEN);
        if (several) {
            content.put("varias_opciones", true);
        }
        if (!wanted.isEmpty() && match && !several) {
            content.put("siguiente_paso", OFFER_TO_BOOK);
        }
        List<ConversationOption> options = several && matched.size() <= MAX_OPTIONS
                ? matched.stream().map(item -> new ConversationOption(OPTION_PREFIX + (item.id() == null ? item.name()
                        : item.id().toString()), item.name())).toList()
                : List.of();
        return new ToolOutcome(content, options, facts);
    }

    private static Map<String, Object> describe(CatalogTreatment item, boolean showPrices, List<String> facts) {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("nombre", item.name());
        facts.add(item.name());
        if (item.category() != null && !item.category().isBlank()) {
            service.put("categoria", item.category());
        }
        if (item.description() != null && !item.description().isBlank()) {
            service.put("descripcion", item.description());
            facts.add(item.description());
        }
        if (item.durationMinutes() != null) {
            service.put("duracion_minutos", item.durationMinutes());
        }
        if (showPrices && item.price() != null) {
            String price = ToolArgs.price(item.price());
            if (item.fixedPrice()) {
                service.put("precio", price);
            } else {
                service.put("precio_desde", price);
                service.put("precio_varia", true);
            }
            facts.add(price);
        } else if (showPrices) {
            service.put("precio", QUOTED_AT_ASSESSMENT);
        }
        return Map.copyOf(service);
    }

    /** Los tratamientos con mas palabras en comun con la busqueda (todos los empatados); vacio si ninguno. */
    private static List<CatalogTreatment> best(List<CatalogTreatment> all, List<String> wanted) {
        int top = 0;
        List<CatalogTreatment> winners = new ArrayList<>();
        for (CatalogTreatment item : all) {
            int score = score(item, wanted);
            if (score > top) {
                top = score;
                winners.clear();
            }
            if (score > 0 && score == top) {
                winners.add(item);
            }
        }
        return List.copyOf(winners);
    }

    private static int score(CatalogTreatment item, List<String> wanted) {
        List<String> own = new ArrayList<>(words(item.name()));
        own.addAll(words(item.category()));
        return (int) wanted.stream().filter(word -> own.stream().anyMatch(candidate -> sameWord(word, candidate))).count();
    }

    private static boolean sameWord(String left, String right) {
        if (left.equals(right)) {
            return true;
        }
        String shorter = left.length() <= right.length() ? left : right;
        String longer = shorter.equals(left) ? right : left;
        return shorter.length() >= MIN_PREFIX && longer.startsWith(shorter);
    }

    /** Palabras utiles sin acentos, en singular aproximado ("limpiezas" y "dentales" como "limpieza" y "dental"). */
    private static List<String> words(String text) {
        return Arrays.stream(ToolArgs.normalize(text).split("[^a-z0-9]+"))
                .filter(word -> word.length() >= MIN_WORD && !FILLER.contains(word))
                .map(ServicesTool::singular)
                .toList();
    }

    private static String singular(String word) {
        if (word.length() > 5 && word.endsWith("es")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.length() > 4 && word.endsWith("s")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }
}
