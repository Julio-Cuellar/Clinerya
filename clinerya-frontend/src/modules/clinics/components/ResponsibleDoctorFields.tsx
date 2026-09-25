import { useState } from "react";
import { IconInfoCircle, IconLock } from "@tabler/icons-react";
import type { ClinicResponse, ResponsibleDoctorRequest } from "@modules/clinics/types";

type OwnerRole = "" | "DOCTOR" | "MANAGER";

// Mismo formato que valida el backend (CedulaProfesional): solo dígitos, entre 5 y 8.
const CEDULA_PATTERN = "\\d{5,8}";
const CEDULA_TITLE = "Solo números, entre 5 y 8 dígitos";

/**
 * Bloque "Médico responsable" del alta de una clínica. Pregunta si el titular de la cuenta es el
 * responsable; si no lo es, si atiende pacientes o solo gestiona la cuenta. Quien atiende
 * pacientes siempre deja su cédula. Va dentro de un <form> y se lee con readResponsibleDoctor.
 */
export function ResponsibleDoctorFields({
  accountHolderName,
  clinic
}: {
  accountHolderName: string;
  clinic?: ClinicResponse;
}) {
  const [ownerIsResponsible, setOwnerIsResponsible] = useState(false);
  const [ownerRole, setOwnerRole] = useState<OwnerRole>("");

  return (
    <section className="responsible-doctor" aria-labelledby="responsible-doctor-title">
      <h3 id="responsible-doctor-title">Médico responsable</h3>

      <label className={`choice-card${ownerIsResponsible ? " selected" : ""}`}>
        <input
          type="checkbox"
          name="ownerIsResponsibleDoctor"
          value="true"
          checked={ownerIsResponsible}
          onChange={(event) => setOwnerIsResponsible(event.target.checked)}
        />
        <span>
          <strong>El titular de la cuenta es el médico responsable</strong>
          <small>Tu cédula se registra en tu perfil: atenderás pacientes y aparecerás en la agenda.</small>
        </span>
      </label>

      <div className="responsible-doctor-grid">
        <label className="field">
          <span>
            Nombre del médico responsable <span className="required-mark">*</span>
          </span>
          {/* Keys distintas: uno es fijo (el titular) y el otro libre; sin ellas React reusa el input. */}
          {ownerIsResponsible ? (
            <input key="account-holder" name="responsibleDoctorName" value={accountHolderName} readOnly />
          ) : (
            <input
              key="other-doctor"
              name="responsibleDoctorName"
              defaultValue={clinic?.responsibleDoctorName}
              placeholder="Ej. Dr. Juan Pérez"
              required
            />
          )}
          {ownerIsResponsible && (
            <small className="field-hint">
              <IconLock size={12} aria-hidden="true" /> Tomado de tu cuenta
            </small>
          )}
        </label>
        <label className="field">
          <span>
            Cédula profesional del responsable <span className="required-mark">*</span>
          </span>
          <input
            name="responsibleDoctorProfessionalLicense"
            defaultValue={clinic?.responsibleDoctorProfessionalLicense}
            placeholder="Ej. 12345678"
            inputMode="numeric"
            pattern={CEDULA_PATTERN}
            title={CEDULA_TITLE}
            required
          />
        </label>
      </div>

      {!ownerIsResponsible && (
        <fieldset className="responsible-doctor-role">
          <legend>
            ¿Cuál es tu papel en la clínica? <span className="required-mark">*</span>
          </legend>
          <label className={`choice-card${ownerRole === "DOCTOR" ? " selected" : ""}`}>
            <input
              type="radio"
              name="ownerRole"
              value="DOCTOR"
              checked={ownerRole === "DOCTOR"}
              onChange={() => setOwnerRole("DOCTOR")}
              required
            />
            <span>
              <strong>Soy médico y atenderé pacientes</strong>
              <small>Te pediremos tu cédula profesional.</small>
            </span>
          </label>
          <label className={`choice-card${ownerRole === "MANAGER" ? " selected" : ""}`}>
            <input
              type="radio"
              name="ownerRole"
              value="MANAGER"
              checked={ownerRole === "MANAGER"}
              onChange={() => setOwnerRole("MANAGER")}
            />
            <span>
              <strong>Solo gestiono la cuenta</strong>
              <small>Administras la clínica sin acceso a expedientes.</small>
            </span>
          </label>

          {ownerRole === "DOCTOR" && (
            <div className="responsible-doctor-grid">
              <label className="field">
                <span>
                  Tu cédula profesional <span className="required-mark">*</span>
                </span>
                <input
                  name="ownerCedulaProfesional"
                  placeholder="Ej. 7654321"
                  inputMode="numeric"
                  pattern={CEDULA_PATTERN}
                  title={CEDULA_TITLE}
                  required
                />
              </label>
            </div>
          )}
          {ownerRole === "MANAGER" && (
            <p className="alert warning responsible-doctor-note">
              <IconInfoCircle size={16} aria-hidden="true" />
              Tendrás caja, inventario, contabilidad, personal y el catálogo de tratamientos. No verás expedientes
              ni podrás cotizar a pacientes. Si después atiendes pacientes, actívalo desde Personal con tu cédula.
            </p>
          )}
        </fieldset>
      )}
    </section>
  );
}

export function readResponsibleDoctor(form: FormData): ResponsibleDoctorRequest {
  const value = (name: string) => String(form.get(name) ?? "").trim();
  const ownerIsResponsibleDoctor = form.get("ownerIsResponsibleDoctor") === "true";
  const ownerRole = value("ownerRole");
  return {
    ownerIsResponsibleDoctor,
    responsibleDoctorName: value("responsibleDoctorName"),
    responsibleDoctorProfessionalLicense: value("responsibleDoctorProfessionalLicense"),
    ownerAttendsPatients: ownerIsResponsibleDoctor ? true : ownerRole ? ownerRole === "DOCTOR" : undefined,
    ownerCedulaProfesional: !ownerIsResponsibleDoctor && ownerRole === "DOCTOR" ? value("ownerCedulaProfesional") : undefined
  };
}
