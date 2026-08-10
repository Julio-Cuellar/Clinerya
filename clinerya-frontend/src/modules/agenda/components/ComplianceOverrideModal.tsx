import { useState } from "react";
import { NOM_SECTIONS } from "@modules/records/constants/nomHistoryTemplate";

const CONFIRM_PHRASE = "Ignorar sugerencias";

export function ComplianceOverrideModal({
  missingSections,
  onCancel,
  onConfirm
}: {
  missingSections: string[];
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const [confirmText, setConfirmText] = useState("");
  const canConfirm = confirmText === CONFIRM_PHRASE;

  const missingTitles = missingSections.map(
    (sectionId) => NOM_SECTIONS.find((section) => section.id === sectionId)?.title ?? sectionId
  );

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Esta plantilla no cumple con la NOM-004</h2>
        </div>
        <p className="confirm-summary-subtitle">
          Las siguientes secciones tienen menos elementos de los sugeridos por la norma:
        </p>
        <ul className="clinic-list">
          {missingTitles.map((title) => (
            <li className="clinic-row" key={title}>
              <strong>{title}</strong>
            </li>
          ))}
        </ul>
        <p className="confirm-summary-subtitle">
          Si continúas, la clínica asume la responsabilidad de que esta plantilla no cubre el mínimo sugerido por la
          NOM-004-SSA3-2012. Para confirmar, escribe exactamente <strong>"{CONFIRM_PHRASE}"</strong> abajo:
        </p>
        <input
          className="compliance-override-input"
          type="text"
          value={confirmText}
          onChange={(event) => setConfirmText(event.target.value)}
          placeholder={CONFIRM_PHRASE}
        />
        <div className="form-actions">
          <button className="btn secondary" type="button" onClick={onCancel}>
            Cancelar
          </button>
          <button className="btn destructive" type="button" disabled={!canConfirm} onClick={onConfirm}>
            Guardar de todas formas
          </button>
        </div>
      </div>
    </div>
  );
}
