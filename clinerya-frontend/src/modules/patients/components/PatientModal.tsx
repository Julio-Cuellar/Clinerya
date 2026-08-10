import { FormEvent, useState } from "react";
import { IconArrowRight, IconX } from "@tabler/icons-react";
import { getFriendlyError, patientsApi } from "@shared/api/api";
import type { Address, BloodType, EmergencyContact, Gender, MaritalStatus, PatientResponse } from "@modules/patients/types";
import { PatientConfirmModal, type PatientDraft } from "@modules/patients/components/PatientConfirmModal";
import { PatientFields } from "@modules/patients/components/PatientFields";

export function PatientModal({
  mode,
  clinicId,
  patient,
  onClose,
  onSaved
}: {
  mode: "create" | "edit";
  clinicId: string;
  patient?: PatientResponse;
  onClose: () => void;
  onSaved: (patient: PatientResponse) => void;
}) {
  const [draft, setDraft] = useState<PatientDraft | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    const form = new FormData(event.currentTarget);
    const value = (name: string) => String(form.get(name) ?? "").trim();

    const address: Address = {
      street: value("addressStreet") || undefined,
      outdoorNumber: value("addressOutdoorNumber") || undefined,
      indoorNumber: value("addressIndoorNumber") || undefined,
      colonia: value("addressColonia") || undefined,
      municipality: value("addressMunicipality") || undefined,
      state: value("addressState") || undefined,
      zipCode: value("addressZipCode") || undefined
    };
    const emergencyContact: EmergencyContact = {
      fullName: value("emergencyContactFullName") || undefined,
      relationship: value("emergencyContactRelationship") || undefined,
      phone: value("emergencyContactPhone") || undefined
    };
    const hasAddress = Object.values(address).some(Boolean);
    const hasEmergencyContact = Object.values(emergencyContact).some(Boolean);

    setDraft({
      firstName: value("firstName"),
      lastNamePaterno: value("lastNamePaterno"),
      lastNameMaterno: value("lastNameMaterno"),
      dateOfBirth: value("dateOfBirth"),
      gender: value("gender") as Gender,
      curp: value("curp") || undefined,
      phone: value("phone") || undefined,
      email: value("email") || undefined,
      occupation: value("occupation") || undefined,
      maritalStatus: (value("maritalStatus") as MaritalStatus) || undefined,
      nationality: value("nationality") || undefined,
      bloodType: (value("bloodType") as BloodType) || undefined,
      address: hasAddress ? address : undefined,
      emergencyContact: hasEmergencyContact ? emergencyContact : undefined
    });
  };

  const confirmSave = async () => {
    if (!draft) return;
    setLoading(true);
    setError("");
    try {
      if (mode === "create") {
        const created = await patientsApi.register({ clinicId, ...draft });
        onSaved(created);
      } else if (patient) {
        const updated = await patientsApi.update(patient.id, draft);
        onSaved(updated);
      }
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-card" onClick={(event) => event.stopPropagation()}>
          <div className="panel-heading">
            <h2>{mode === "create" ? "Nuevo paciente" : "Editar paciente"}</h2>
            <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
              <IconX size={18} />
            </button>
          </div>
          <form className="profile-form" onSubmit={submit}>
            <PatientFields patient={patient} />

            <div className="form-actions">
              <button className="btn primary" type="submit">
                <IconArrowRight size={20} aria-hidden="true" />
                Continuar
              </button>
            </div>
          </form>
        </div>
      </div>

      {draft && (
        <PatientConfirmModal
          mode={mode}
          draft={draft}
          loading={loading}
          error={error}
          onConfirm={confirmSave}
          onEdit={() => {
            setDraft(null);
            setError("");
          }}
        />
      )}
    </>
  );
}
