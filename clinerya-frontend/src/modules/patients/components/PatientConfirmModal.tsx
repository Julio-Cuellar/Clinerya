import { IconCheck, IconPencil } from "@tabler/icons-react";
import type { Address, BloodType, EmergencyContact, Gender, MaritalStatus } from "@modules/patients/types";
import { PatientSummaryList } from "@modules/patients/components/PatientSummaryList";

export interface PatientDraft {
  firstName: string;
  lastNamePaterno: string;
  lastNameMaterno: string;
  dateOfBirth: string;
  gender: Gender;
  curp?: string;
  phone?: string;
  email?: string;
  occupation?: string;
  maritalStatus?: MaritalStatus;
  nationality?: string;
  bloodType?: BloodType;
  address?: Address;
  emergencyContact?: EmergencyContact;
}

export function PatientConfirmModal({
  mode,
  draft,
  loading,
  error,
  onConfirm,
  onEdit
}: {
  mode: "create" | "edit";
  draft: PatientDraft;
  loading: boolean;
  error: string;
  onConfirm: () => void;
  onEdit: () => void;
}) {
  return (
    <div className="modal-overlay" onClick={onEdit}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>¿Los datos son correctos?</h2>
        </div>
        <p className="confirm-summary-subtitle">
          Revisa la información antes de {mode === "create" ? "registrar" : "guardar los cambios de"}:
        </p>
        <p className="confirm-summary-name">
          {draft.firstName} {draft.lastNamePaterno} {draft.lastNameMaterno}
        </p>

        <PatientSummaryList patient={draft} />

        {error && <p className="alert error">{error}</p>}

        <div className="form-actions">
          <button className="btn secondary" type="button" onClick={onEdit} disabled={loading}>
            <IconPencil size={18} aria-hidden="true" />
            Editar
          </button>
          <button className="btn primary" type="button" onClick={onConfirm} disabled={loading}>
            <IconCheck size={18} aria-hidden="true" />
            {loading ? "Guardando" : "Confirmar"}
          </button>
        </div>
      </div>
    </div>
  );
}
