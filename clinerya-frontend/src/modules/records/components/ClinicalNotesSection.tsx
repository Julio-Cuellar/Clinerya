import { useEffect, useMemo, useState } from "react";
import { IconPlus, IconSearch } from "@tabler/icons-react";
import { clinicalNotesApi, getFriendlyError } from "@shared/api/api";
import type { ClinicalNoteResponse, NoteStatus } from "@modules/records/types";
import { ClinicalNoteModal } from "@modules/records/components/ClinicalNoteModal";

const statusLabel: Record<NoteStatus, string> = {
  DRAFT: "Borrador",
  SIGNED: "Firmada"
};

const statusBadge: Record<NoteStatus, string> = {
  DRAFT: "warning",
  SIGNED: "success"
};

function authorLabel(note: ClinicalNoteResponse): string {
  if (note.doctorName && note.doctorName.trim()) return note.doctorName.trim();
  if (note.authoredByExternalUserId) return "Especialista externo";
  return "Staff de la clínica";
}

function previewLine(note: ClinicalNoteResponse): string {
  const text = (note.assessment || note.plan || note.subjective || note.objective || "").replace(/\s+/g, " ").trim();
  if (!text) return "Sin contenido capturado";
  return text.length > 140 ? `${text.slice(0, 137)}...` : text;
}

export function ClinicalNotesSection({
  patientId,
  clinicId,
  canWriteNotes = true,
  defaultDoctorId
}: {
  patientId: string;
  clinicId: string;
  canWriteNotes?: boolean;
  defaultDoctorId?: string;
}) {
  const [notes, setNotes] = useState<ClinicalNoteResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [creating, setCreating] = useState(false);
  const [activeNote, setActiveNote] = useState<ClinicalNoteResponse | null>(null);
  const [search, setSearch] = useState("");
  const [doctorFilter, setDoctorFilter] = useState("todos");

  const load = () => {
    setLoading(true);
    setError("");
    clinicalNotesApi
      .listByPatient(patientId, clinicId)
      .then(setNotes)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [patientId, clinicId]);

  const doctorOptions = useMemo(() => {
    const seen = new Map<string, string>();
    notes.forEach((note) => {
      if (note.doctorId) seen.set(note.doctorId, authorLabel(note));
    });
    return Array.from(seen, ([id, label]) => ({ id, label }));
  }, [notes]);

  const visibleNotes = useMemo(() => {
    const term = search.trim().toLowerCase();
    return notes.filter((note) => {
      if (doctorFilter !== "todos" && note.doctorId !== doctorFilter) return false;
      if (!term) return true;
      return [note.assessment, note.plan, note.subjective, note.objective, authorLabel(note)]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
        .includes(term);
    });
  }, [notes, search, doctorFilter]);

  return (
    <div className="history-notes-section">
      <div className="panel-heading">
        <h3>Notas clínicas</h3>
        {canWriteNotes && (
          <button className="btn secondary" type="button" onClick={() => setCreating(true)}>
            <IconPlus size={16} aria-hidden="true" />
            Nueva nota
          </button>
        )}
      </div>

      {!loading && notes.length > 0 && (
        <div className="table-toolbar">
          <label className="search-field">
            <IconSearch size={16} aria-hidden="true" />
            <input
              type="search"
              placeholder="Buscar en evaluación, plan, notas…"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>
          {doctorOptions.length > 1 && (
            <label className="field">
              <span>Médico</span>
              <select value={doctorFilter} onChange={(event) => setDoctorFilter(event.target.value)}>
                <option value="todos">Todos</option>
                {doctorOptions.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          )}
        </div>
      )}

      {loading && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Cargando notas...</strong>
          </div>
        </div>
      )}

      {!loading && notes.length === 0 && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Sin notas registradas</strong>
            <span>Aquí aparecerán las notas clínicas de este paciente</span>
          </div>
        </div>
      )}

      {!loading && notes.length > 0 && visibleNotes.length === 0 && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Sin resultados</strong>
            <span>Ninguna nota coincide con el filtro actual</span>
          </div>
        </div>
      )}

      {!loading && visibleNotes.length > 0 && (
        <div className="clinic-list">
          {visibleNotes.map((note) => (
            <div className="clinic-row clinic-row-actionable" key={note.id} onClick={() => setActiveNote(note)}>
              <div className="note-row-main">
                <strong>{new Date(note.createdAt).toLocaleString("es-MX", { dateStyle: "medium", timeStyle: "short" })}</strong>
                <span>{authorLabel(note)}</span>
                <span className="note-row-preview">{previewLine(note)}</span>
              </div>
              <div className="clinic-row-actions">
                <span className={`badge ${statusBadge[note.status]}`}>{statusLabel[note.status]}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {error && <p className="alert error">{error}</p>}

      {creating && (
        <ClinicalNoteModal
          patientId={patientId}
          clinicId={clinicId}
          defaultDoctorId={defaultDoctorId}
          onClose={() => setCreating(false)}
          onSaved={() => {
            setCreating(false);
            load();
          }}
        />
      )}

      {activeNote && (
        <ClinicalNoteModal
          patientId={patientId}
          clinicId={clinicId}
          existingNote={activeNote}
          defaultDoctorId={defaultDoctorId}
          onClose={() => setActiveNote(null)}
          onSaved={() => {
            setActiveNote(null);
            load();
          }}
          readOnly={!canWriteNotes}
        />
      )}
    </div>
  );
}
