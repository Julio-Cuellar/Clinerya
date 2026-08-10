import { IconArrowRight, IconCheck, IconClipboardList } from "@tabler/icons-react";
import { moduleOnboardingCopy } from "@app/constants/moduleOnboarding";
import { modules, type ModuleKey } from "@app/constants/modules";

export function ModuleWelcomeScreen({
  moduleKey,
  onStart,
  compact = false
}: {
  moduleKey: ModuleKey;
  onStart: () => void;
  compact?: boolean;
}) {
  const copy = moduleOnboardingCopy[moduleKey];
  const moduleInfo = modules.find((item) => item.key === moduleKey);
  const ModuleIcon = moduleInfo?.icon ?? IconClipboardList;
  const titleId = `${moduleKey}-welcome-title`;

  return (
    <section className={`module-welcome ${compact ? "module-welcome-compact" : ""}`} aria-labelledby={titleId}>
      <div className="module-welcome-main">
        <div className="module-welcome-intro">
          <span className="module-welcome-badge">{copy.eyebrow}</span>
          <div className="module-welcome-icon">
            <ModuleIcon size={34} strokeWidth={1.8} aria-hidden="true" />
          </div>
          <div className="module-welcome-copy">
            <h2 id={titleId}>{copy.title}</h2>
            <p>{copy.description}</p>
          </div>
          <button className="btn primary" type="button" onClick={onStart}>
            {copy.primaryLabel}
            <IconArrowRight size={16} aria-hidden="true" />
          </button>
        </div>

        <div className="module-welcome-details" aria-label="Recorrido inicial">
          <div className="module-welcome-section">
            <h3>Que encontraras</h3>
            <ul>
              {copy.highlights.map((item) => (
                <li key={item}>
                  <IconCheck size={16} aria-hidden="true" />
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="module-welcome-section">
            <h3>Configuracion inicial</h3>
            <ol>
              {copy.setupSteps.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ol>
          </div>
        </div>
      </div>
    </section>
  );
}
