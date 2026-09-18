import { useState } from "react";
import { IconDeviceFloppy, IconInfoCircle } from "@tabler/icons-react";
import type { ClinicResponse, ClinicSpecialty } from "@modules/clinics/types";
import { ClinicSpecialtyPicker } from "@modules/clinics/components/ClinicSpecialtyPicker";
import { clinicsApi, getFriendlyError } from "@shared/api/api";

export function ClinicSpecialtyPanel({
  clinic,
  onChanged
}: {
  clinic: ClinicResponse;
  onChanged: (updated: ClinicResponse) => void;
}) {
  const current = clinic.specialty ?? "SIN_CONFIGURAR";
  const [selected, setSelected] = useState<ClinicSpecialty>(current);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const dirty = selected !== current;

  const save = async () => {
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const updated = await clinicsApi.updateSpecialty(clinic.id, { specialty: selected });
      onChanged(updated);
      setSuccess("Especialidad actualizada.");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="panel" style={{ padding: "16px", background: "var(--color-card)" }}>
      <div className="panel-heading">
        <div>
          <h3>Especialidad de la clinica</h3>
          <p className="description" style={{ fontSize: "12px", color: "var(--color-text-3)", marginTop: "4px" }}>
            Ajusta el vocabulario del modulo de tratamientos, que campos se capturan y que plantilla de
            historia clinica se recomienda.
          </p>
        </div>
      </div>

      {current === "SIN_CONFIGURAR" && (
        <p className="alert warning" style={{ marginBottom: "12px" }}>
          Esta clinica todavia no tiene especialidad. Mientras tanto se comporta como generica.
        </p>
      )}

      <ClinicSpecialtyPicker selected={selected} onSelect={setSelected} />

      {dirty && (
        <p className="alert info" style={{ display: "flex", gap: "8px", alignItems: "flex-start" }}>
          <IconInfoCircle size={18} aria-hidden="true" style={{ flexShrink: 0, marginTop: "1px" }} />
          <span>
            Cambiar de especialidad no borra nada. Las cotizaciones ya registradas conservan sus datos,
            incluido el numero de diente, aunque deje de mostrarse.
          </span>
        </p>
      )}

      {error && <p className="alert error">{error}</p>}
      {success && <p className="alert success">{success}</p>}

      <div className="form-actions">
        <button className="btn primary" type="button" disabled={saving || !dirty} onClick={save}>
          <IconDeviceFloppy size={18} aria-hidden="true" />
          {saving ? "Guardando..." : "Guardar especialidad"}
        </button>
      </div>
    </div>
  );
}
