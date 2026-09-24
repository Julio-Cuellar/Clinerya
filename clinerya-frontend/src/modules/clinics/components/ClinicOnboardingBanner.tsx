import { IconArrowRight, IconSettingsBolt } from "@tabler/icons-react";
import type { ClinicOnboardingState } from "@modules/clinics/lib/clinicOnboarding";

export function ClinicOnboardingBanner({
  state,
  onContinue,
  onDismiss
}: {
  state: ClinicOnboardingState;
  onContinue: () => void;
  onDismiss: () => void;
}) {
  const detail = !state.specialtyDone
    ? "Falta elegir la especialidad de la clínica, que define el vocabulario y los campos del módulo de tratamientos."
    : "Falta armar tu catálogo de servicios; podemos sembrarlo con el sugerido de tu especialidad.";

  return (
    <div className="onboarding-banner">
      <div className="onboarding-banner-icon" aria-hidden="true">
        <IconSettingsBolt size={18} />
      </div>
      <div className="onboarding-banner-text">
        <strong>Termina de configurar tu clínica</strong>
        <span>{detail}</span>
      </div>
      <div className="onboarding-banner-actions">
        <button className="btn primary" type="button" onClick={onContinue}>
          Continuar
          <IconArrowRight size={16} aria-hidden="true" />
        </button>
        <button className="btn ghost" type="button" onClick={onDismiss}>
          Más tarde
        </button>
      </div>
    </div>
  );
}
