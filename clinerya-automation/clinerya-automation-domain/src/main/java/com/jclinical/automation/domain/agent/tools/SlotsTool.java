package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;
import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort;
import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort.CatalogTreatment;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;
import com.jclinical.automation.domain.service.SlotLabel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Horarios libres reales de un medico, como opciones para elegir (hasta 10). El medico se valida
 * contra el directorio de la clinica antes de buscar: el modelo no puede inventar uno.
 */
public final class SlotsTool implements AgentTool {

    public static final String NAME = "buscar_horarios";
    public static final String OPTION_PREFIX = "slot:";
    static final int MAX_OPTIONS = 10;
    static final int MAX_DAYS = 14;
    private static final int DEFAULT_DAYS = 7;
    private static final int SEARCH_LIMIT = 60;

    private final DoctorDirectoryPort doctors;
    private final SlotAvailabilityPort slots;
    private final TreatmentCatalogPort catalog;

    public SlotsTool(DoctorDirectoryPort doctors, SlotAvailabilityPort slots, TreatmentCatalogPort catalog) {
        this.doctors = doctors;
        this.slots = slots;
        this.catalog = catalog;
    }

    public SlotsTool(DoctorDirectoryPort doctors, SlotAvailabilityPort slots) {
        this(doctors, slots, null);
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Busca horarios libres reales de un médico. Aparecen como lista para que el paciente elija.",
                List.of(new ToolSpec.Parameter("medico", "string",
                                "Id de la opción del médico (doctor:...). Puede omitirse si la clínica tiene un solo médico.", false),
                        new ToolSpec.Parameter("desde", "string", "Fecha desde la que se busca, AAAA-MM-DD. Por omisión, hoy.", false),
                        new ToolSpec.Parameter("dias", "integer", "Cuántos días buscar, de 1 a 14. Por omisión, 7.", false),
                        new ToolSpec.Parameter("turno", "string", "mañana, tarde o noche, si el paciente lo pidió.", false),
                        new ToolSpec.Parameter("servicio", "string", "Id de la opción del servicio (servicio:...) o su nombre: "
                                + "los horarios duran lo que el servicio.", false)));
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        List<DoctorContact> all = doctors.listDoctors(context.clinicId());
        String requested = ToolArgs.text(arguments, "medico");
        Optional<DoctorContact> doctor;
        if (requested.isEmpty()) {
            if (all.isEmpty()) {
                return ToolOutcome.of(Map.of("error", "La clínica no tiene médicos disponibles para agendar."));
            }
            if (all.size() > 1) {
                return ToolOutcome.of(Map.of("error", "Primero hay que elegir con qué médico.",
                        "medicos", all.stream().map(DoctorContact::displayName).toList())).withOptions(DoctorsTool.options(all));
            }
            doctor = Optional.of(all.getFirst());
        } else {
            UUID id = parseId(requested);
            doctor = all.stream().filter(candidate -> candidate.staffId().equals(id)).findFirst();
        }
        if (doctor.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "Ese médico no atiende en la clínica."));
        }

        Optional<CatalogTreatment> service = Optional.empty();
        String requestedService = ToolArgs.text(arguments, "servicio");
        if (!requestedService.isEmpty() && catalog != null) {
            service = findService(context.clinicId(), requestedService);
            if (service.isEmpty()) {
                return ToolOutcome.of(Map.of("error", "Ese servicio no se agenda por aquí; revisa los servicios disponibles."));
            }
        }
        Integer duration = service.map(CatalogTreatment::durationMinutes).orElse(null);
        String serviceSuffix = service.map(CatalogTreatment::id).map(id -> "|" + id).orElse("");

        LocalDate today = context.now().toLocalDate();
        LocalDate requestedFrom = ToolArgs.date(arguments, "desde");
        LocalDate from = requestedFrom == null || requestedFrom.isBefore(today) ? today : requestedFrom;
        int days = Math.max(1, Math.min(MAX_DAYS, ToolArgs.integer(arguments, "dias", DEFAULT_DAYS)));
        Predicate<LocalDateTime> daypart = daypart(ToolArgs.text(arguments, "turno"));
        UUID doctorId = doctor.get().staffId();
        List<AvailableSlot> found = slots.availableSlots(context.clinicId(), doctorId, from, days, SEARCH_LIMIT, duration).stream()
                .filter(slot -> slot.start().isAfter(context.now()) && daypart.test(slot.start()))
                .limit(MAX_OPTIONS)
                .toList();

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("medico", doctor.get().displayName());
        service.ifPresent(chosen -> {
            content.put("servicio", chosen.name());
            content.put("duracion_minutos", chosen.durationMinutes());
        });
        if (found.isEmpty()) {
            content.put("sin_horarios", true);
            content.put("sugerencia", "Prueba otro día, otro turno o buscar más días.");
            return new ToolOutcome(content, List.of(), List.of(doctor.get().displayName()));
        }
        List<ConversationOption> options = found.stream()
                .map(slot -> new ConversationOption(OPTION_PREFIX + doctorId + "|" + slot.start() + "|" + slot.end()
                        + serviceSuffix, SlotLabel.of(slot.start())))
                .toList();
        List<String> labels = options.stream().map(ConversationOption::label).toList();
        content.put("horarios", labels);
        List<String> facts = new ArrayList<>(labels);
        facts.add(doctor.get().displayName());
        return new ToolOutcome(content, options, facts);
    }

    /** Horario de una opcion "slot:medico|inicio|fin[|servicio]"; {@code serviceId} puede faltar. */
    record ChosenSlot(UUID doctorId, LocalDateTime start, LocalDateTime end, UUID serviceId) {}

    static ChosenSlot parse(String optionId) {
        String[] parts = optionId.substring(OPTION_PREFIX.length()).split("[|]");
        UUID serviceId = parts.length > 3 && !parts[3].isBlank() ? UUID.fromString(parts[3]) : null;
        return new ChosenSlot(UUID.fromString(parts[0]), LocalDateTime.parse(parts[1]), LocalDateTime.parse(parts[2]),
                serviceId);
    }

    /** Por el id de la opcion (servicio:uuid) o por su nombre, entre los servicios del asistente. */
    private Optional<CatalogTreatment> findService(UUID clinicId, String requested) {
        String raw = requested.startsWith(ServicesTool.OPTION_PREFIX)
                ? requested.substring(ServicesTool.OPTION_PREFIX.length()).trim() : requested.trim();
        String name = ToolArgs.normalize(raw);
        return catalog.activeTreatments(clinicId).stream()
                .filter(item -> item.durationMinutes() != null)
                .filter(item -> (item.id() != null && item.id().toString().equals(raw)) || ToolArgs.normalize(item.name()).equals(name))
                .findFirst();
    }

    private static UUID parseId(String requested) {
        String raw = requested.startsWith(DoctorsTool.OPTION_PREFIX)
                ? requested.substring(DoctorsTool.OPTION_PREFIX.length()) : requested;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException notAnId) {
            return null;
        }
    }

    private static Predicate<LocalDateTime> daypart(String requested) {
        return switch (ToolArgs.normalize(requested)) {
            case "manana" -> start -> start.getHour() < 12;
            case "tarde" -> start -> start.getHour() >= 12 && start.getHour() < 19;
            case "noche" -> start -> start.getHour() >= 19;
            default -> start -> true;
        };
    }
}
