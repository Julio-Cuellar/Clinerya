import { useEffect, useState } from "react";
import { IconPlus } from "@tabler/icons-react";
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

      {!loading && notes.length > 0 && (
        <div className="clinic-list">
          {notes.map((note) => (
            <div className="clinic-row clinic-row-actionable" key={note.id} onClick={() => setActiveNote(note)}>
              <div>
                <strong>{new Date(note.createdAt).toLocaleString()}</strong>
                <span>{note.authoredByExternalUserId ? "Escrita por especialista externo" : "Escrita por el staff"}</span>
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
