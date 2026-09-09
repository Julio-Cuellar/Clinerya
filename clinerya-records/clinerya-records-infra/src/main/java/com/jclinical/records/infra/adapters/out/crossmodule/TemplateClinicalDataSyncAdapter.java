package com.jclinical.records.infra.adapters.out.crossmodule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.records.domain.model.AllergyCategory;
import com.jclinical.records.domain.model.AllergySeverity;
import com.jclinical.records.domain.model.ClinicalDataSource;
import com.jclinical.records.domain.model.ConditionStatus;
import com.jclinical.records.domain.model.PatientAllergy;
import com.jclinical.records.domain.model.PatientCondition;
import com.jclinical.records.domain.model.PatientMedication;
import com.jclinical.records.domain.ports.out.PatientAllergyRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientConditionRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientMedicationRepositoryPort;
import com.jclinical.records.domain.ports.out.TemplateClinicalDataSyncPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Vuelca a datos clínicos tipados los campos de una plantilla marcados con
 * {@code clinicalMapping} en su {@code schemaJson}. Reemplaza las filas con
 * {@code source = TEMPLATE} de cada tipo mapeado; las capturadas a mano
 * ({@code MANUAL}) nunca se tocan. JSON mal formado = no hace nada.
 *
 * <p>Forma esperada del mapeo en un elemento del schema:
 * <pre>
 * { "id": "alergias", "type": "table",
 *   "clinicalMapping": { "target": "ALLERGY", "primary": 0, "secondary": 1,
 *                        "tertiary": 2, "defaultCategory": "DRUG" } }
 * </pre>
 * Semántica de columnas: ALLERGY = sustancia / reacción; CONDITION = nombre /
 * CIE-10 / fecha de inicio; MEDICATION = nombre / dosis / frecuencia.
 */
@Component
@RequiredArgsConstructor
public class TemplateClinicalDataSyncAdapter implements TemplateClinicalDataSyncPort {

    private final ObjectMapper objectMapper;
    private final PatientAllergyRepositoryPort allergyRepository;
    private final PatientConditionRepositoryPort conditionRepository;
    private final PatientMedicationRepositoryPort medicationRepository;

    private enum Target { ALLERGY, CONDITION, MEDICATION }

    private record Mapping(String fieldId, Target target, int primary, int secondary, int tertiary,
                           AllergyCategory defaultCategory) {}

    @Override
    @Transactional
    public void sync(UUID clinicId, UUID patientId, String schemaJson, String answersJson,
                     UUID actingUserId, String actingUserName) {
        JsonNode schema = parse(schemaJson);
        JsonNode answers = parse(answersJson);
        if (!schema.isObject()) {
            return;
        }

        List<Mapping> mappings = collectMappings(schema);
        if (mappings.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        syncAllergies(clinicId, patientId, answers, mappings, actingUserId, actingUserName, now);
        syncConditions(clinicId, patientId, answers, mappings, actingUserId, actingUserName, now);
        syncMedications(clinicId, patientId, answers, mappings, actingUserId, actingUserName, now);
    }

    private void syncAllergies(UUID clinicId, UUID patientId, JsonNode answers, List<Mapping> mappings,
                               UUID userId, String userName, LocalDateTime now) {
        List<Mapping> maps = mappings.stream().filter(m -> m.target() == Target.ALLERGY).toList();
        if (maps.isEmpty()) {
            return;
        }
        allergyRepository.deleteByClinicIdAndPatientIdAndSource(clinicId, patientId, ClinicalDataSource.TEMPLATE);
        for (Mapping m : maps) {
            for (List<String> row : rowsFor(answers, m.fieldId())) {
                String substance = cell(row, m.primary());
                if (substance.isEmpty()) {
                    continue;
                }
                allergyRepository.save(PatientAllergy.builder()
                        .id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId)
                        .substance(substance)
                        .reaction(blankToNull(cell(row, m.secondary())))
                        .severity(AllergySeverity.UNKNOWN)
                        .category(m.defaultCategory() != null ? m.defaultCategory() : AllergyCategory.OTHER)
                        .source(ClinicalDataSource.TEMPLATE)
                        .notedByUserId(userId).notedByUserName(userName).notedAt(now)
                        .build());
            }
        }
    }

    private void syncConditions(UUID clinicId, UUID patientId, JsonNode answers, List<Mapping> mappings,
                                UUID userId, String userName, LocalDateTime now) {
        List<Mapping> maps = mappings.stream().filter(m -> m.target() == Target.CONDITION).toList();
        if (maps.isEmpty()) {
            return;
        }
        conditionRepository.deleteByClinicIdAndPatientIdAndSource(clinicId, patientId, ClinicalDataSource.TEMPLATE);
        for (Mapping m : maps) {
            for (List<String> row : rowsFor(answers, m.fieldId())) {
                String name = cell(row, m.primary());
                if (name.isEmpty()) {
                    continue;
                }
                conditionRepository.save(PatientCondition.builder()
                        .id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId)
                        .name(name)
                        .icd10Code(blankToNull(cell(row, m.secondary())))
                        .status(ConditionStatus.ACTIVE)
                        .onsetDate(parseDate(cell(row, m.tertiary())))
                        .source(ClinicalDataSource.TEMPLATE)
                        .notedByUserId(userId).notedByUserName(userName).notedAt(now)
                        .build());
            }
        }
    }

    private void syncMedications(UUID clinicId, UUID patientId, JsonNode answers, List<Mapping> mappings,
                                 UUID userId, String userName, LocalDateTime now) {
        List<Mapping> maps = mappings.stream().filter(m -> m.target() == Target.MEDICATION).toList();
        if (maps.isEmpty()) {
            return;
        }
        medicationRepository.deleteByClinicIdAndPatientIdAndSource(clinicId, patientId, ClinicalDataSource.TEMPLATE);
        for (Mapping m : maps) {
            for (List<String> row : rowsFor(answers, m.fieldId())) {
                String name = cell(row, m.primary());
                if (name.isEmpty()) {
                    continue;
                }
                medicationRepository.save(PatientMedication.builder()
                        .id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId)
                        .medicationName(name)
                        .dose(blankToNull(cell(row, m.secondary())))
                        .schedule(blankToNull(cell(row, m.tertiary())))
                        .active(true)
                        .source(ClinicalDataSource.TEMPLATE)
                        .notedByUserId(userId).notedByUserName(userName).notedAt(now)
                        .build());
            }
        }
    }

    // ---- parseo del schema y de las respuestas -------------------------------

    private List<Mapping> collectMappings(JsonNode schema) {
        List<Mapping> mappings = new ArrayList<>();
        JsonNode pages = schema.path("pages");
        if (pages.isArray()) {
            pages.forEach(page -> collectFromElements(page.path("elements"), mappings));
        }
        collectFromElements(schema.path("elements"), mappings);
        return mappings;
    }

    private void collectFromElements(JsonNode elements, List<Mapping> mappings) {
        if (!elements.isArray()) {
            return;
        }
        for (JsonNode element : elements) {
            JsonNode mapping = element.path("clinicalMapping");
            if (!mapping.isObject()) {
                continue;
            }
            Target target = parseTarget(mapping.path("target").asText(""));
            if (target == null) {
                continue;
            }
            String fieldId = element.path("id").asText("");
            if (fieldId.isEmpty()) {
                continue;
            }
            mappings.add(new Mapping(
                    fieldId,
                    target,
                    mapping.path("primary").asInt(0),
                    mapping.path("secondary").asInt(1),
                    mapping.path("tertiary").asInt(2),
                    target == Target.ALLERGY ? parseCategory(mapping.path("defaultCategory").asText("")) : null));
        }
    }

    /** Devuelve las filas (listas de celdas) de un campo. Tabla = JSON de string[][]; texto = una fila de una celda. */
    private List<List<String>> rowsFor(JsonNode answers, String fieldId) {
        JsonNode value = answers.path(fieldId);
        if (value.isArray()) {
            return toRows(value, null);
        }
        String raw = value.asText("");
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return List.of(List.of(raw.trim()));
        }
        return toRows(parsed, raw);
    }

    private static List<List<String>> toRows(JsonNode parsed, String rawFallback) {
        if (parsed.isArray()) {
            if (parsed.isEmpty()) {
                return List.of();
            }
            if (parsed.get(0).isArray()) {
                List<List<String>> rows = new ArrayList<>();
                for (JsonNode rowNode : parsed) {
                    List<String> cells = new ArrayList<>();
                    rowNode.forEach(cellNode -> cells.add(cellNode.asText("")));
                    rows.add(cells);
                }
                return rows;
            }
            // Array de escalares = una sola fila.
            List<String> cells = new ArrayList<>();
            parsed.forEach(cellNode -> cells.add(cellNode.asText("")));
            return List.of(cells);
        }
        if (parsed.isTextual()) {
            return List.of(List.of(parsed.asText().trim()));
        }
        return rawFallback != null ? List.of(List.of(rawFallback.trim())) : List.of();
    }

    private JsonNode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private static Target parseTarget(String value) {
        try {
            return Target.valueOf(value.trim().toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static AllergyCategory parseCategory(String value) {
        try {
            return value.isBlank() ? null : AllergyCategory.valueOf(value.trim().toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return value.isEmpty() ? null : LocalDate.parse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String cell(List<String> row, int index) {
        return index >= 0 && index < row.size() && row.get(index) != null ? row.get(index).trim() : "";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
