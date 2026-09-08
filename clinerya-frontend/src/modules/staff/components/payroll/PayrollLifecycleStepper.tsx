import { PAYROLL_STAGE_STEPS, type PayrollStage } from "@modules/staff/lib/payroll";

const stageOrder: PayrollStage[] = ["capture", "review", "closed", "paid"];

/** Capturar -> Revisar -> Cerrar -> Pagar, with the current stage highlighted. */
export function PayrollLifecycleStepper({ stage }: { stage: PayrollStage }) {
  const currentIndex = stageOrder.indexOf(stage);
  return (
    <ol className="payroll-stepper" aria-label="Ciclo de la nomina">
      {PAYROLL_STAGE_STEPS.map((step, index) => {
        const state = index < currentIndex ? "done" : index === currentIndex ? "current" : "upcoming";
        return (
          <li key={step.key} className={`payroll-stepper-step is-${state}`} aria-current={state === "current" ? "step" : undefined}>
            <span className="payroll-stepper-index">{index + 1}</span>
            <span className="payroll-stepper-label">{step.label}</span>
          </li>
        );
      })}
    </ol>
  );
}
