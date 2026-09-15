import { IconStethoscope } from "@tabler/icons-react";
import {
  CLINICAL_MAPPING_CATEGORY_LABELS,
  CLINICAL_MAPPING_COLUMN_ROLES,
  CLINICAL_MAPPING_TARGET_LABELS,
  type ClinicalMapping,
  type ClinicalMappingCategory,
  type ClinicalMappingTarget,
  type TemplateElement
} from "@modules/records/types";

const MAPPABLE_TYPES: ReadonlyArray<TemplateElement["type"]> = ["table", "text", "textarea"];

/**
 * Control para conectar un campo de la plantilla con los datos clínicos tipados
 * del paciente. Solo se muestra para tablas y campos de texto: al guardar la
 * historia, el backend vuelca las respuestas a alergias / padecimientos / medicación.
 */
export function ClinicalMappingToolbar({
  element,
  onChange
}: {
  element: TemplateElement;
  onChange: (next: TemplateElement) => void;
}) {
  if (!MAPPABLE_TYPES.includes(element.type)) {
    return null;
  }

  const mapping = element.clinicalMapping;
  const isTable = element.type === "table";
  const columns = element.columns ?? [];

  const setTarget = (value: string) => {
    if (!value) {
      const next = { ...element };
      delete next.clinicalMapping;
      onChange(next);
      return;
    }
    const target = value as ClinicalMappingTarget;
    const next: ClinicalMapping = {
      target,
      primary: mapping?.primary ?? 0,
      secondary: mapping?.secondary ?? 1,
      tertiary: mapping?.tertiary ?? 2,
      defaultCategory: target === "ALLERGY" ? mapping?.defaultCategory : undefined
    };
    onChange({ ...element, clinicalMapping: next });
  };

  const setColumn = (role: "primary" | "secondary" | "tertiary", value: string) => {
    if (!mapping) return;
    onChange({ ...element, clinicalMapping: { ...mapping, [role]: Number(value) } });
  };

  const setCategory = (value: string) => {
    if (!mapping) return;
    onChange({
      ...element,
      clinicalMapping: {
        ...mapping,
        defaultCategory: value ? (value as ClinicalMappingCategory) : undefined
      }
    });
  };

  const roles = mapping ? CLINICAL_MAPPING_COLUMN_ROLES[mapping.target] : null;

  return (
    <div className="typography-toolbar clinical-mapping-toolbar">
      <span className="clinical-mapping-label">
        <IconStethoscope size={15} aria-hidden="true" />
        Dato clínico
      </span>

      <select
        value={mapping?.target ?? ""}
        onChange={(event) => setTarget(event.target.value)}
        aria-label="Mapear campo a dato clínico"
      >
        <option value="">Sin mapeo</option>
        {(Object.keys(CLINICAL_MAPPING_TARGET_LABELS) as ClinicalMappingTarget[]).map((target) => (
          <option key={target} value={target}>
            {CLINICAL_MAPPING_TARGET_LABELS[target]}
          </option>
        ))}
      </select>

      {mapping && isTable && roles && columns.length > 0 &&
        (["primary", "secondary", "tertiary"] as const).map((role, index) => {
          const roleLabel = roles[index];
          if (!roleLabel) return null;
          return (
            <label key={role} className="clinical-mapping-column">
              <span>{roleLabel}</span>
              <select value={mapping[role] ?? index} onChange={(event) => setColumn(role, event.target.value)}>
                {columns.map((column, columnIndex) => (
                  <option key={columnIndex} value={columnIndex}>
                    {column || `Columna ${columnIndex + 1}`}
                  </option>
                ))}
              </select>
            </label>
          );
        })}

      {mapping && isTable && columns.length === 0 && (
        <span className="clinical-mapping-hint">Define las columnas de la tabla para elegir cuál alimenta cada dato.</span>
      )}

      {mapping?.target === "ALLERGY" && (
        <label className="clinical-mapping-column">
          <span>Categoría</span>
          <select value={mapping.defaultCategory ?? ""} onChange={(event) => setCategory(event.target.value)}>
            <option value="">Sin categoría</option>
            {(Object.keys(CLINICAL_MAPPING_CATEGORY_LABELS) as ClinicalMappingCategory[]).map((category) => (
              <option key={category} value={category}>
                {CLINICAL_MAPPING_CATEGORY_LABELS[category]}
              </option>
            ))}
          </select>
        </label>
      )}
    </div>
  );
}
