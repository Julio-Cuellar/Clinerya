import { useEffect, useState } from "react";
import { IconDeviceFloppy, IconPlus, IconTrash, IconX } from "@tabler/icons-react";
import { visitsApi, materialsApi, getFriendlyError } from "@shared/api/api";
import type { QuotationResponse, CreateVisitRequest } from "@modules/treatments/quotationTypes";
import type { MaterialResponse } from "@modules/inventory/types";

interface RegisterVisitModalProps {
  patientId: string;
  quotation: QuotationResponse;
  clinicId: string;
  onClose: () => void;
  onSaved: () => void;
}

interface SelectedMaterialUsage {
  materialId?: string;
  materialName: string;
  actualQuantity: string;
}

interface ItemSessionState {
  quotationItemId: string;
  description: string;
  attended: boolean;
  materialsUsed: SelectedMaterialUsage[];
}

export function RegisterVisitModal({
  patientId,
  quotation,
  clinicId,
  onClose,
  onSaved
}: RegisterVisitModalProps) {
  const [visitDate, setVisitDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [notes, setNotes] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [materialsList, setMaterialsList] = useState<MaterialResponse[]>([]);

  const [itemsState, setItemsState] = useState<ItemSessionState[]>(() => {
    return quotation.items.map((item) => ({
      quotationItemId: item.id,
      description: item.description,
      attended: false,
      materialsUsed: item.materials.map((m) => ({
        materialId: m.materialId,
        materialName: m.materialName,
        actualQuantity: String(m.estimatedQuantity)
      }))
    }));
  });

  useEffect(() => {
    materialsApi.list(clinicId)
      .then(setMaterialsList)
      .catch((err) => console.error("Error loading clinic materials", err));
  }, [clinicId]);

  const handleAttendedChange = (itemId: string, checked: boolean) => {
    setItemsState((prev) =>
      prev.map((item) => (item.quotationItemId === itemId ? { ...item, attended: checked } : item))
    );
  };

  const handleQuantityChange = (itemId: string, index: number, quantity: string) => {
    setItemsState((prev) =>
      prev.map((item) => {
        if (item.quotationItemId !== itemId) return item;
        const nextMaterials = [...item.materialsUsed];
        nextMaterials[index] = { ...nextMaterials[index], actualQuantity: quantity };
        return { ...item, materialsUsed: nextMaterials };
      })
    );
  };

  const handleRemoveMaterial = (itemId: string, index: number) => {
    setItemsState((prev) =>
      prev.map((item) => {
        if (item.quotationItemId !== itemId) return item;
        return {
          ...item,
          materialsUsed: item.materialsUsed.filter((_, idx) => idx !== index)
        };
      })
    );
  };

  const handleAddMaterial = (itemId: string, materialId: string) => {
    if (!materialId) return;

    const matched = materialsList.find((m) => m.id === materialId);
    if (!matched) return;

    setItemsState((prev) =>
      prev.map((item) => {
        if (item.quotationItemId !== itemId) return item;
        if (item.materialsUsed.some((m) => m.materialId === materialId)) return item;
        return {
          ...item,
          materialsUsed: [
            ...item.materialsUsed,
            {
              materialId: matched.id,
              materialName: matched.name,
              actualQuantity: "1"
            }
          ]
        };
      })
    );
  };

  const handleAddCustomMaterial = (itemId: string) => {
    setItemsState((prev) =>
      prev.map((item) => {
        if (item.quotationItemId !== itemId) return item;
        return {
          ...item,
          materialsUsed: [
            ...item.materialsUsed,
            {
              materialName: "Material personalizado",
              actualQuantity: "1"
            }
          ]
        };
      })
    );
  };

  const handleCustomMaterialNameChange = (itemId: string, index: number, name: string) => {
    setItemsState((prev) =>
      prev.map((item) => {
        if (item.quotationItemId !== itemId) return item;
        const nextMaterials = [...item.materialsUsed];
        nextMaterials[index] = { ...nextMaterials[index], materialName: name };
        return { ...item, materialsUsed: nextMaterials };
      })
    );
  };

  const handleSave = async () => {
    setError("");
    const activeItems = itemsState.filter((item) => item.attended);
    if (activeItems.length === 0) {
      setError("Debes marcar al menos un tratamiento como atendido en esta visita.");
      return;
    }

    for (const item of activeItems) {
      for (const m of item.materialsUsed) {
        const qty = parseFloat(m.actualQuantity);
        if (isNaN(qty) || qty <= 0) {
          setError(`La cantidad de '${m.materialName}' en el tratamiento '${item.description}' debe ser un número mayor a cero.`);
          return;
        }
      }
    }

    setSaving(true);
    try {
      const body: CreateVisitRequest = {
        clinicId,
        visitDate,
        notes: notes.trim() || undefined,
        items: activeItems.map((item) => ({
          quotationItemId: item.quotationItemId,
          materialsUsed: item.materialsUsed.map((m) => ({
            materialId: m.materialId,
            materialName: m.materialName,
            actualQuantity: parseFloat(m.actualQuantity)
          }))
        }))
      };

      await visitsApi.create(patientId, quotation.id, body);
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()} style={{ maxWidth: '800px' }}>
        <div className="panel-heading" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2>Registrar Sesión de Tratamiento</h2>
          <button type="button" className="icon-btn" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        <div className="panel-body" style={{ maxHeight: '65vh', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          {error && <p className="alert error">{error}</p>}

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: 'var(--space-4)' }}>
            <label className="field">
              <span>Fecha de la sesión</span>
              <input type="date" value={visitDate} onChange={(e) => setVisitDate(e.target.value)} />
            </label>
          </div>

          <label className="field">
            <span>Notas de la sesión / Observaciones clínicas</span>
            <textarea
              placeholder="Describa la evolución del tratamiento, indicaciones o notas de la sesión..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              style={{ minHeight: '80px', resize: 'vertical' }}
            />
          </label>

          <div>
            <h3 style={{ fontSize: '14px', marginBottom: 'var(--space-3)', borderBottom: '1px solid var(--color-border)', paddingBottom: '4px' }}>
              Tratamientos atendidos y consumo de materiales
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
              {itemsState.map((item) => {
                const originalItem = quotation.items.find((i) => i.id === item.quotationItemId);

                return (
                  <div 
                    key={item.quotationItemId} 
                    style={{
                      border: '1px solid var(--color-border)',
                      borderRadius: '6px',
                      padding: 'var(--space-3)',
                      backgroundColor: item.attended ? 'var(--color-surface)' : 'var(--color-bg-1)'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <input
                        type="checkbox"
                        id={`chk-${item.quotationItemId}`}
                        checked={item.attended}
                        onChange={(e) => handleAttendedChange(item.quotationItemId, e.target.checked)}
                        style={{ cursor: 'pointer' }}
                      />
                      <label 
                        htmlFor={`chk-${item.quotationItemId}`} 
                        style={{ fontWeight: 600, fontSize: '13px', cursor: 'pointer', margin: 0 }}
                      >
                        {item.description} {originalItem?.toothNumber ? `(Pieza ${originalItem.toothNumber})` : ''}
                      </label>
                    </div>

                    {item.attended && (
                      <div style={{ marginTop: 'var(--space-3)', paddingLeft: 'var(--space-5)' }}>
                        <h4 style={{ fontSize: '12px', color: 'var(--color-text-2)', marginBottom: '8px' }}>
                          Materiales consumidos
                        </h4>

                        {item.materialsUsed.length === 0 ? (
                          <p style={{ fontSize: '12px', color: 'var(--color-text-3)', margin: '0 0 10px 0' }}>
                            No hay materiales registrados para este tratamiento.
                          </p>
                        ) : (
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '12px' }}>
                            {item.materialsUsed.map((m, idx) => {
                              const originalMat = originalItem?.materials.find((om) => om.materialId === m.materialId);

                              return (
                                <div 
                                  key={idx} 
                                  style={{
                                    display: 'grid',
                                    gridTemplateColumns: '3fr 1.5fr 1.5fr auto',
                                    alignItems: 'center',
                                    gap: '10px'
                                  }}
                                >
                                  {m.materialId ? (
                                    <span style={{ fontSize: '13px' }}>{m.materialName}</span>
                                  ) : (
                                    <input
                                      type="text"
                                      value={m.materialName}
                                      placeholder="Nombre de material personalizado"
                                      onChange={(e) => handleCustomMaterialNameChange(item.quotationItemId, idx, e.target.value)}
                                      style={{ fontSize: '12px', padding: '4px 8px' }}
                                    />
                                  )}

                                  <span style={{ fontSize: '11px', color: 'var(--color-text-3)' }}>
                                    {originalMat ? `Presupuestado: ${originalMat.estimatedQuantity}` : 'No presupuestado'}
                                  </span>

                                  <label style={{ display: 'flex', alignItems: 'center', gap: '4px', margin: 0 }}>
                                    <span style={{ fontSize: '11px' }}>Uso:</span>
                                    <input
                                      type="number"
                                      step="any"
                                      value={m.actualQuantity}
                                      onChange={(e) => handleQuantityChange(item.quotationItemId, idx, e.target.value)}
                                      style={{ width: '60px', padding: '4px', fontSize: '12px', textAlign: 'center' }}
                                    />
                                  </label>

                                  <button
                                    type="button"
                                    className="icon-btn text-danger"
                                    aria-label="Quitar material"
                                    onClick={() => handleRemoveMaterial(item.quotationItemId, idx)}
                                  >
                                    <IconTrash size={14} />
                                  </button>
                                </div>
                              );
                            })}
                          </div>
                        )}

                        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                          <select
                            defaultValue=""
                            onChange={(e) => {
                              handleAddMaterial(item.quotationItemId, e.target.value);
                              e.target.value = "";
                            }}
                            style={{ maxWidth: '220px', padding: '4px', fontSize: '12px' }}
                          >
                            <option value="">+ Añadir del inventario...</option>
                            {materialsList
                              .filter((mat) => !item.materialsUsed.some((um) => um.materialId === mat.id))
                              .map((mat) => (
                                <option key={mat.id} value={mat.id}>
                                  {mat.name} ({mat.currentStock} disp.)
                                </option>
                              ))}
                          </select>

                          <button
                            type="button"
                            className="btn ghost"
                            onClick={() => handleAddCustomMaterial(item.quotationItemId)}
                            style={{ padding: '4px 8px', fontSize: '11px' }}
                          >
                            <IconPlus size={12} /> Personalizado
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        <div className="form-actions" style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', padding: 'var(--space-4)' }}>
          <button type="button" className="btn ghost" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button type="button" className="btn primary" onClick={handleSave} disabled={saving}>
            <IconDeviceFloppy size={16} />
            {saving ? "Registrando..." : "Guardar sesión"}
          </button>
        </div>
      </div>
    </div>
  );
}
