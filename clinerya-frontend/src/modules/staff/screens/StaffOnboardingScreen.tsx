import { useState } from "react";
import { IconArrowLeft, IconUserPlus } from "@tabler/icons-react";
import {
  getFriendlyError,
  staffApi,
  type StaffCompensationRequest,
  type StaffPayFrequency,
  type StaffPaymentMethod
} from "@shared/api/api";
import { CurrencyInput } from "@modules/staff/components/payroll/CurrencyInput";

const payFrequencyLabels: Record<StaffPayFrequency, string> = {
  WEEKLY: "Semanal",
  BIWEEKLY: "Quincenal",
  MONTHLY: "Mensual"
};

const paymentMethodLabels: Record<StaffPaymentMethod, string> = {
  BANK_TRANSFER: "Transferencia",
  CASH: "Efectivo"
};

/** Página de alta de personal: invita a un nuevo integrante y adjunta sus datos de nómina. */
export function StaffOnboardingScreen({
  clinicId,
  canManagePayroll,
  onBack,
  onSaved
}: {
  clinicId: string;
  canManagePayroll: boolean;
  onBack: () => void;
  onSaved: () => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [baseSalary, setBaseSalary] = useState<number | null>(null);
  const [payFrequency, setPayFrequency] = useState<StaffPayFrequency>("BIWEEKLY");
  const [paymentMethod, setPaymentMethod] = useState<StaffPaymentMethod>("BANK_TRANSFER");
  const [paymentAccountClabe, setPaymentAccountClabe] = useState("");
  const [rfc, setRfc] = useState("");
  const [curp, setCurp] = useState("");
  const [nss, setNss] = useState("");

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "").trim();
    const role = String(form.get("role") ?? "DOCTOR");

    const compensation: StaffCompensationRequest | null = canManagePayroll
      ? {
          baseSalary: baseSalary ?? 0,
          payFrequency,
          paymentMethod,
          paymentAccountClabe: paymentAccountClabe.trim() || null,
          rfc: rfc.trim() || null,
          curp: curp.trim() || null,
          nss: nss.trim() || null
        }
      : null;

    try {
      const invitation = await staffApi.invite(clinicId, { email, role });
      if (compensation) {
        await staffApi.setInvitationCompensation(clinicId, invitation.invitationId, compensation);
      }
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="dashboard-grid">
      <article className="panel full">
        <div className="panel-heading">
          <div>
            <h2>Agregar personal a la clínica</h2>
            <span className="panel-subtitle">
              Envía una invitación para que el nuevo integrante complete su registro con sus datos.
              El correo no debe estar registrado previamente en la plataforma.
            </span>
          </div>
          <button className="btn ghost" type="button" onClick={onBack}>
            <IconArrowLeft size={16} aria-hidden="true" />
            Volver a personal
          </button>
        </div>

        <form className="profile-form" onSubmit={submit}>
          <label className="field field-full">
            <span>Correo del usuario</span>
            <input name="email" type="email" required placeholder="usuario@ejemplo.com" />
          </label>

          <label className="field field-full">
            <span>Rol</span>
            <select name="role" defaultValue="DOCTOR">
              <option value="DOCTOR">Doctor / Especialista</option>
              <option value="RECEPTIONIST">Recepcionista</option>
              <option value="ASSISTANT">Asistente médico</option>
              <option value="ADMIN">Administrador de sistema</option>
              <option value="CLINIC_ADMIN">Administrador de clínica</option>
              <option value="ACCOUNTANT">Contador</option>
              <option value="CLEANING">Personal de limpieza</option>
            </select>
          </label>

          {canManagePayroll && (
            <>
              <div className="field field-full">
                <strong style={{ fontSize: "13px" }}>Datos de nómina</strong>
                <small className="description">Se aplican al confirmar el registro del empleado.</small>
              </div>
              <label className="field">
                <span>Sueldo base</span>
                <CurrencyInput ariaLabel="Sueldo base" value={baseSalary} onValueChange={setBaseSalary} />
              </label>
              <label className="field">
                <span>Periodicidad</span>
                <select value={payFrequency} onChange={(event) => setPayFrequency(event.target.value as StaffPayFrequency)}>
                  {(Object.keys(payFrequencyLabels) as StaffPayFrequency[]).map((value) => (
                    <option key={value} value={value}>{payFrequencyLabels[value]}</option>
                  ))}
                </select>
              </label>
              <label className="field">
                <span>Método de pago</span>
                <select value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value as StaffPaymentMethod)}>
                  {(Object.keys(paymentMethodLabels) as StaffPaymentMethod[]).map((value) => (
                    <option key={value} value={value}>{paymentMethodLabels[value]}</option>
                  ))}
                </select>
              </label>
              <label className="field">
                <span>CLABE</span>
                <input value={paymentAccountClabe} maxLength={18} placeholder="18 dígitos" onChange={(event) => setPaymentAccountClabe(event.target.value)} />
              </label>
              <label className="field">
                <span>RFC</span>
                <input value={rfc} maxLength={13} onChange={(event) => setRfc(event.target.value.toUpperCase())} />
              </label>
              <label className="field">
                <span>CURP</span>
                <input value={curp} maxLength={18} onChange={(event) => setCurp(event.target.value.toUpperCase())} />
              </label>
              <label className="field">
                <span>NSS</span>
                <input value={nss} maxLength={11} onChange={(event) => setNss(event.target.value)} />
              </label>
            </>
          )}

          {error && <p className="alert error">{error}</p>}
          <div className="form-actions" style={{ marginTop: "20px" }}>
            <button className="btn primary" disabled={loading} type="submit">
              <IconUserPlus size={18} aria-hidden="true" style={{ marginRight: "6px" }} />
              {loading ? "Enviando..." : "Enviar invitación"}
            </button>
            <button className="btn ghost" type="button" onClick={onBack}>
              Cancelar
            </button>
          </div>
        </form>
      </article>
    </section>
  );
}
