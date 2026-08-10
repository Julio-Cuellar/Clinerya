import { useEffect, useMemo, useState } from "react";
import { IconDental, IconPlus, IconSearch, IconWand } from "@tabler/icons-react";
import { buildNomStarterPages } from "@modules/records/constants/nomHistoryTemplate";
import { buildNomOdontologyStarterPages } from "@modules/records/constants/nomOdontologyTemplate";
import { getFriendlyError, historyTemplatesApi } from "@shared/api/api";
import { parseSchema, serializeSchema, type HistoryTemplateResponse } from "@modules/records/types";
import { TemplateModal } from "@modules/records/components/TemplateModal";

export function TemplatesPanel({ clinicId, hasClinic }: { clinicId?: string; hasClinic: boolean }) {
  const [templates, setTemplates] = useState<HistoryTemplateResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<HistoryTemplateResponse | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = () => {
    if (!clinicId) return;
    setLoading(true);
    historyTemplatesApi
      .list(clinicId)
      .then(setTemplates)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId]);

  const filteredTemplates = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return templates;
    return templates.filter((template) => template.name.toLowerCase().includes(term));
  }, [templates, search]);

  const hasHistoriaClinica = templates.some(
    (template) => template.active && parseSchema(template.schemaJson).kind === "historia_clinica"
  );

  const handleUseRecommended = async () => {
    if (!clinicId) return;
    setBusyId("recommended");
    setError("");
    try {
      const schema = {
        kind: "historia_clinica" as const,
        pages: buildNomStarterPages()
      };
      const created = await historyTemplatesApi.create(clinicId, {
        name: "Historia clínica general (NOM-004)",
        description: "Plantilla base sugerida por la NOM-004-SSA3-2012, editable a criterio de la clínica.",
        schemaJson: serializeSchema(schema)
      });
      setTemplates((prev) => [...prev, created]);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusyId(null);
    }
  };

  const handleUseOdontologyRecommended = async () => {
    if (!clinicId) return;
    setBusyId("recommended-odontologia");
    setError("");
    try {
      const schema = {
        kind: "historia_clinica" as const,
        pages: buildNomOdontologyStarterPages()
      };
      const created = await historyTemplatesApi.create(clinicId, {
        name: "Historia clínica odontológica (NOM-013)",
        description:
          "Plantilla base para consultorios dentales basada en el formato del cliente. Incluye antecedentes, interrogatorio por sistemas, odontograma, indice CPOD, consentimiento de tratamiento, contrato de servicios y consentimiento de uso de la app.",
        schemaJson: serializeSchema(schema)
      });
      setTemplates((prev) => [...prev, created]);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusyId(null);
    }
  };

  const handleToggleActive = async (template: HistoryTemplateResponse) => {
    setBusyId(template.id);
    setError("");
    try {
      if (template.active) {
        await historyTemplatesApi.remove(clinicId!, template.id);
        setTemplates((prev) => prev.map((item) => (item.id === template.id ? { ...item, active: false } : item)));
      } else {
        const updated = await historyTemplatesApi.update(clinicId!, template.id, {
          name: template.name,
          description: template.description,
          schemaJson: template.schemaJson,
          active: true
        });
        setTemplates((prev) => prev.map((item) => (item.id === template.id ? updated : item)));
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
        <h2>Plantillas de expediente</h2>
        <div className="topbar-actions">
          <span className="badge neutral">{templates.length}</span>
          {!hasHistoriaClinica && hasClinic && (
            <button className="btn secondary" type="button" disabled={busyId === "recommended"} onClick={handleUseRecommended}>
              <IconWand size={16} aria-hidden="true" />
              {busyId === "recommended" ? "Creando..." : "Usar plantilla NOM-004 recomendada"}
            </button>
          )}
          {!hasHistoriaClinica && hasClinic && (
            <button
              className="btn secondary"
              type="button"
              disabled={busyId === "recommended-odontologia"}
              onClick={handleUseOdontologyRecommended}
            >
              <IconDental size={16} aria-hidden="true" />
              {busyId === "recommended-odontologia" ? "Creando..." : "Usar plantilla NOM-013 (Odontología) recomendada"}
            </button>
          )}
          <button className="btn primary" type="button" disabled={!hasClinic} onClick={() => setCreating(true)}>
            <IconPlus size={16} aria-hidden="true" />
            Nueva plantilla
          </button>
        </div>
      </div>

      {hasClinic && templates.length > 0 && (
        <div className="table-toolbar">
          <label className="search-field">
            <IconSearch size={16} aria-hidden="true" />
            <input type="search" placeholder="Buscar plantilla" value={search} onChange={(event) => setSearch(event.target.value)} />
          </label>
        </div>
      )}

      {!hasClinic && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Completa los datos de tu clínica</strong>
            <span>Necesitas una clínica activa para crear plantillas</span>
          </div>
        </div>
      )}
      {hasClinic && loading && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Cargando plantillas...</strong>
          </div>
        </div>
      )}
      {hasClinic && !loading && templates.length === 0 && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Sin plantillas registradas</strong>
            <span>Usa la plantilla NOM-004 recomendada o crea una nueva</span>
          </div>
        </div>
      )}

      {hasClinic && !loading && templates.length > 0 && (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Tipo</th>
                <th>Cumplimiento</th>
                <th>Estado</th>
                <th aria-label="Acciones" />
              </tr>
            </thead>
            <tbody>
              {filteredTemplates.map((template) => {
                const schema = parseSchema(template.schemaJson);
                const isHistoriaClinica = schema.kind === "historia_clinica";
                return (
                  <tr key={template.id} onClick={() => setEditing(template)}>
                    <td>
                      <strong>{template.name}</strong>
                      {template.description && <div>{template.description}</div>}
                    </td>
                    <td>{isHistoriaClinica ? "Historia clínica" : "Historia clínica personalizada"}</td>
                    <td>
                      {isHistoriaClinica ? (
                        schema.compliance?.overridden ? (
                          <span className="badge warning">Con excepciones</span>
                        ) : (
                          <span className="badge success">Cumple NOM-004</span>
                        )
                      ) : (
                        "—"
                      )}
                    </td>
                    <td>
                      <span className={`badge ${template.active ? "success" : "neutral"}`}>
                        {template.active ? "Activa" : "Inactiva"}
                      </span>
                    </td>
                    <td className="table-actions">
                      <button className="btn ghost" type="button" onClick={(event) => { event.stopPropagation(); setEditing(template); }}>
                        Editar
                      </button>
                      <button
                        className="btn destructive"
                        type="button"
                        disabled={busyId === template.id}
                        onClick={(event) => {
                          event.stopPropagation();
                          handleToggleActive(template);
                        }}
                      >
                        {busyId === template.id ? "..." : template.active ? "Desactivar" : "Activar"}
                      </button>
                    </td>
                  </tr>
                );
              })}
              {filteredTemplates.length === 0 && (
                <tr>
                  <td colSpan={5}>Sin resultados para "{search}"</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {error && <p className="alert error">{error}</p>}

      {creating && clinicId && (
        <TemplateModal
          clinicId={clinicId}
          onClose={() => setCreating(false)}
          onSaved={(created) => {
            setTemplates((prev) => [...prev, created]);
            setCreating(false);
          }}
        />
      )}

      {editing && clinicId && (
        <TemplateModal
          clinicId={clinicId}
          template={editing}
          onClose={() => setEditing(null)}
          onSaved={(updated) => {
            setTemplates((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
            setEditing(null);
          }}
        />
      )}
    </article>
  );
}
