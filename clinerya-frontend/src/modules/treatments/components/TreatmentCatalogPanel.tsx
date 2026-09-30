import { useEffect, useMemo, useState } from "react";
import { assistantStatus, catalogWarning, priceLabel } from "../logic/catalogRules";
import { IconInfoCircle, IconPlus, IconSearch, IconWand } from "@tabler/icons-react";
import { getFriendlyError, treatmentCatalogApi } from "@shared/api/api";
import type { TreatmentCatalogItemResponse } from "@modules/treatments/types";
import { TreatmentCatalogModal } from "@modules/treatments/components/TreatmentCatalogModal";
import { DEFAULT_CLINIC_PROFILE, type ClinicProfile } from "@shared/utils/clinicProfile";

const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });

export function TreatmentCatalogPanel({
  clinicId,
  hasClinic,
  profile = DEFAULT_CLINIC_PROFILE
}: {
  clinicId?: string;
  hasClinic: boolean;
  profile?: ClinicProfile;
}) {
  const [items, setItems] = useState<TreatmentCatalogItemResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<TreatmentCatalogItemResponse | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [seeding, setSeeding] = useState(false);

  const handleSeed = async () => {
    if (!clinicId) return;
    setSeeding(true);
    setError("");
    try {
      const result = await treatmentCatalogApi.seed(clinicId);
      setItems(result.items);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSeeding(false);
    }
  };

  const load = () => {
    if (!clinicId) return;
    setLoading(true);
    treatmentCatalogApi
      .list(clinicId, true)
      .then(setItems)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId]);

  const filteredItems = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return items;
    return items.filter(
      (item) => item.name.toLowerCase().includes(term) || (item.category ?? "").toLowerCase().includes(term)
    );
  }, [items, search]);

  const handleToggleActive = async (item: TreatmentCatalogItemResponse) => {
    setBusyId(item.id);
    setError("");
    try {
      if (item.active) {
        await treatmentCatalogApi.remove(clinicId!, item.id);
        setItems((prev) => prev.map((current) => (current.id === item.id ? { ...current, active: false } : current)));
      } else {
        const updated = await treatmentCatalogApi.update(clinicId!, item.id, {
          name: item.name,
          category: item.category,
          description: item.description,
          defaultPrice: item.defaultPrice,
          estimatedDurationMinutes: item.estimatedDurationMinutes,
          materials: item.materials.map((material) => ({
            materialId: material.materialId,
            typicalQuantity: material.typicalQuantity
          })),
          active: true,
          pricingType: item.pricingType,
          availableInAssistant: item.availableInAssistant
        });
        setItems((prev) => prev.map((current) => (current.id === item.id ? updated : current)));
      }
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <article className="panel full">
      <div className="panel-heading">
        <h2>Catálogo de tratamientos y servicios</h2>
        <div className="topbar-actions">
          <span className="badge neutral">{items.length}</span>
          <button className="btn primary" type="button" disabled={!hasClinic} onClick={() => setCreating(true)}>
            <IconPlus size={16} aria-hidden="true" />
            Nuevo servicio
          </button>
        </div>
      </div>

      {hasClinic && items.length > 0 && (
        <div className="table-toolbar">
          <label className="search-field">
            <IconSearch size={16} aria-hidden="true" />
            <input
              type="search"
              placeholder="Buscar por nombre o categoría"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>
        </div>
      )}

      {!hasClinic && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Completa los datos de tu clínica</strong>
            <span>Necesitas una clínica activa para crear el catálogo de tratamientos</span>
          </div>
        </div>
      )}
      {hasClinic && loading && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Cargando catálogo...</strong>
          </div>
        </div>
      )}
      {hasClinic && !loading && items.length === 0 && (
        <div className="catalog-empty-state">
          <strong>Empieza tu catálogo de servicios</strong>
          {profile.hasSuggestedCatalog ? (
            <>
              <p>
                Podemos crear el catálogo sugerido para {profile.label.toLowerCase()} y lo ajustas desde
                aquí, o lo armas tú desde cero.
              </p>
              <p className="catalog-empty-note">
                <IconInfoCircle size={16} aria-hidden="true" />
                Los precios son de referencia. Ajústalos a tu clínica antes de cotizar.
              </p>
              <div className="catalog-empty-actions">
                <button className="btn primary" type="button" disabled={seeding} onClick={handleSeed}>
                  <IconWand size={16} aria-hidden="true" />
                  {seeding ? "Creando..." : `Usar catálogo de ${profile.label.toLowerCase()}`}
                </button>
                <button className="btn" type="button" onClick={() => setCreating(true)}>
                  Empezar en blanco
                </button>
              </div>
            </>
          ) : (
            <p>
              Crea el primer servicio o tratamiento de tu catálogo. Si eliges una especialidad en la
              configuración de la clínica, podemos sugerirte un catálogo de arranque.
            </p>
          )}
        </div>
      )}

      {hasClinic && !loading && catalogWarning(items) && (
        <p className="alert warning catalog-warning" role="status">
          <strong>{catalogWarning(items)}</strong> Ábrelos con "Editar" para completarlos.
        </p>
      )}

      {hasClinic && !loading && items.length > 0 && (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Categoría</th>
                <th>Precio</th>
                <th>Duración</th>
                <th>Asistente</th>
                <th>Estado</th>
                <th aria-label="Acciones" />
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item) => (
                <tr key={item.id} onClick={() => setEditing(item)}>
                  <td>
                    <strong>{item.name}</strong>
                    {item.description && <div>{item.description}</div>}
                  </td>
                  <td>{item.category || "—"}</td>
                  <td>
                    {priceLabel(item)}{" "}
                    <span className={`badge ${item.pricingType === "VARIES_BY_PATIENT" ? "info" : "success"}`}>
                      {item.pricingType === "VARIES_BY_PATIENT" ? "Varía" : "Fijo"}
                    </span>
                  </td>
                  <td>{item.estimatedDurationMinutes ? `${item.estimatedDurationMinutes} min` : "—"}</td>
                  <td>
                    <span className={`badge ${assistantStatus(item).tone === "ok" ? "success" : assistantStatus(item).tone === "warn" ? "warning" : "neutral"}`}>
                      {assistantStatus(item).label}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${item.active ? "success" : "neutral"}`}>
                      {item.active ? "Activo" : "Inactivo"}
                    </span>
                  </td>
                  <td className="table-actions">
                    <button className="btn ghost" type="button" onClick={(event) => { event.stopPropagation(); setEditing(item); }}>
                      Editar
                    </button>
                    <button
                      className="btn destructive"
                      type="button"
                      disabled={busyId === item.id}
                      onClick={(event) => {
                        event.stopPropagation();
                        handleToggleActive(item);
                      }}
                    >
                      {busyId === item.id ? "..." : item.active ? "Desactivar" : "Activar"}
                    </button>
                  </td>
                </tr>
              ))}
              {filteredItems.length === 0 && (
                <tr>
                  <td colSpan={7}>Sin resultados para "{search}"</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {error && <p className="alert error">{error}</p>}

      {creating && clinicId && (
        <TreatmentCatalogModal
          clinicId={clinicId}
          profile={profile}
          onClose={() => setCreating(false)}
          onSaved={(created) => {
            setItems((prev) => [...prev, created]);
            setCreating(false);
          }}
        />
      )}

      {editing && clinicId && (
        <TreatmentCatalogModal
          clinicId={clinicId}
          item={editing}
          profile={profile}
          onClose={() => setEditing(null)}
          onSaved={(updated) => {
            setItems((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
            setEditing(null);
          }}
        />
      )}
    </article>
  );
}
