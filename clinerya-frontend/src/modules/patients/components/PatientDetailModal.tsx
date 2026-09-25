import { useEffect, useState } from "react";
import { IconX } from "@tabler/icons-react";
import { staffApi } from "@shared/api/api";
import type { PatientResponse } from "@modules/patients/types";
import { PatientSummaryList } from "@modules/patients/components/PatientSummaryList";
import { ContactConsentCard } from "@modules/patients/components/ContactConsentCard";

export function PatientDetailModal({
  patient,
  onClose,
  onUpdated
}: {
  patient: PatientResponse;
  onClose: () => void;
  onUpdated: (patient: PatientResponse) => void;
}) {
  const [staffNames, setStaffNames] = useState<Record<string, string>>({});

  // Para mostrar quien registro el consentimiento. Sin permiso de ver personal la lista falla y la
  // tarjeta muestra un nombre generico: no es motivo para bloquear la ficha.
  useEffect(() => {
    staffApi
      .list(patient.clinicId)
      .then((staff) => setStaffNames(Object.fromEntries(staff.map((member) => [member.userId, member.fullName]))))
      .catch(() => setStaffNames({}));
  }, [patient.clinicId]);

  const recordedBy = patient.contactConsent?.recordedByUserId;

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
        <ContactConsentCard
          patient={patient}
          recordedByName={recordedBy ? staffNames[recordedBy] : undefined}
          onUpdated={onUpdated}
        />
      </div>
    </div>
  );
}
