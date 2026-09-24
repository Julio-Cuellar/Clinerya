import { FormEvent, useEffect, useState } from "react";
import { IconDental, IconPlus, IconTrash, IconX } from "@tabler/icons-react";
import { getFriendlyError, materialsApi, treatmentCatalogApi } from "@shared/api/api";
import type { TreatmentCatalogItemResponse } from "@modules/treatments/types";
import type { MaterialResponse } from "@modules/inventory/types";
import { Field } from "@shared/ui/Field";
import { DEFAULT_CLINIC_PROFILE, type ClinicProfile } from "@shared/utils/clinicProfile";

interface EditableMaterialLine {
  key: string;
  materialId: string;
  typicalQuantity: string;
}

function emptyMaterialLine(): EditableMaterialLine {
  return {
    key: `catmat_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    materialId: "",
    typicalQuantity: ""
  };
}

export function TreatmentCatalogModal({
  clinicId,
  item,
  profile = DEFAULT_CLINIC_PROFILE,
  onClose,
  onSaved
}: {
  clinicId: string;
  item?: TreatmentCatalogItemResponse;
  profile?: ClinicProfile;
  onClose: () => void;
  onSaved: (item: TreatmentCatalogItemResponse) => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [materials, setMaterials] = useState<MaterialResponse[]>([]);
  const [materialLines, setMaterialLines] = useState<EditableMaterialLine[]>(() =>
    item && item.materials.length > 0
      ? item.materials.map((material) => ({
          key: material.id,
          materialId: material.materialId,
          typicalQuantity: material.typicalQuantity ? String(material.typicalQuantity) : ""
        }))
      : []
  );

  useEffect(() => {
    materialsApi
      .list(clinicId, false)
      .then(setMaterials)
      .catch((caught) => setError(getFriendlyError(caught)));
  }, [clinicId]);

  const updateLine = (key: string, patch: Partial<EditableMaterialLine>) => {
    setMaterialLines((prev) => prev.map((line) => (line.key === key ? { ...line, ...patch } : line)));
  };

  const removeLine = (key: string) => {
    setMaterialLines((prev) => prev.filter((line) => line.key !== key));
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    const form = new FormData(event.currentTarget);
    const value = (name: string) => String(form.get(name) ?? "").trim();

    const materialsPayload = materialLines
      .filter((line) => line.materialId)
      .map((line) => ({
        materialId: line.materialId,
        typicalQuantity: line.typicalQuantity ? Number(line.typicalQuantity) : undefined
      }));

    try {
      if (item) {
        const updated = await treatmentCatalogApi.update(clinicId, item.id, {
          name: value("name"),
          category: value("category") || undefined,
          description: value("description") || undefined,
          defaultPrice: Number(value("defaultPrice")) || 0,
          estimatedDurationMinutes: value("estimatedDurationMinutes") ? Number(value("estimatedDurationMinutes")) : undefined,
          materials: materialsPayload,
          active: item.active
        });
        onSaved(updated);
      } else {
        const created = await treatmentCatalogApi.create(clinicId, {
          name: value("name"),
          category: value("category") || undefined,
          description: value("description") || undefined,
          defaultPrice: Number(value("defaultPrice")) || 0,
          estimatedDurationMinutes: value("estimatedDurationMinutes") ? Number(value("estimatedDurationMinutes")) : undefined,
          materials: materialsPayload
        });
        onSaved(created);
      }
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>{item ? "Editar servicio" : "Nuevo servicio"}</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <form className="profile-form" onSubmit={submit}>
          <Field name="name" label="Nombre del servicio" defaultValue={item?.name} required />
          <Field name="category" label="Categoría" defaultValue={item?.category} placeholder="Preventiva, Restaurativa, Cirugía..." />
          <Field
            name="defaultPrice"
            label={profile.laborPriceLabel}
            type="number"
            defaultValue={item ? String(item.defaultPrice) : undefined}
            required
          />
          <Field
            name="estimatedDurationMinutes"
            label="Duración estimada (minutos)"
            type="number"
            defaultValue={item?.estimatedDurationMinutes ? String(item.estimatedDurationMinutes) : undefined}
          />
          <label className="field field-full">
            <span>Descripción</span>
            <textarea name="description" defaultValue={item?.description} />
          </label>

          <div className="quotation-items-editor field-full">
            <span>Checklist típico de materiales (opcional, no obliga cantidad exacta al cotizar)</span>
            <div className="table-wrapper">
              <table className="data-table no-row-click">
                <thead>
                  <tr>
                    <th>Material</th>
                    <th>Cantidad típica</th>
                    <th aria-label="Quitar" />
                  </tr>
                </thead>
                <tbody>
                  {materialLines.map((line) => (
                    <tr key={line.key}>
                      <td>
                        <select
                          value={line.materialId}
                          onChange={(event) => updateLine(line.key, { materialId: event.target.value })}
                        >
                          <option value="">Selecciona un material</option>
                          {materials.map((material) => (
                            <option key={material.id} value={material.id}>
                              {material.name}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td>
                        <input
                          type="number"
                          min="0.0001"
                          step="0.0001"
                          placeholder="Opcional"
                          value={line.typicalQuantity}
                          onChange={(event) => updateLine(line.key, { typicalQuantity: event.target.value })}
                        />
                      </td>
                      <td>
                        <button
                          className="icon-btn"
                          type="button"
                          aria-label="Quitar material"
                          onClick={() => removeLine(line.key)}
                        >
                          <IconTrash size={16} />
                        </button>
                      </td>
                    </tr>
                  ))}
                  {materialLines.length === 0 && (
                    <tr>
                      <td colSpan={3}>
                        <div className="empty-table-state">Sin materiales en el checklist.</div>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            <button className="btn ghost" type="button" onClick={() => setMaterialLines((prev) => [...prev, emptyMaterialLine()])}>
              <IconPlus size={16} aria-hidden="true" />
              Agregar material
            </button>
          </div>

          {error && <p className="alert error">{error}</p>}
          <div className="form-actions">
            <button className="btn primary" disabled={loading} type="submit">
              <IconDental size={18} aria-hidden="true" />
              {loading ? "Guardando" : "Guardar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
