import { IconX } from "@tabler/icons-react";
import type { PatientResponse } from "@modules/patients/types";
import { PatientSummaryList } from "@modules/patients/components/PatientSummaryList";

export function PatientDetailModal({ patient, onClose }: { patient: PatientResponse; onClose: () => void }) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>
            {patient.firstName} {patient.lastNamePaterno} {patient.lastNameMaterno}
          </h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <PatientSummaryList patient={patient} />
      </div>
    </div>
  );
}
