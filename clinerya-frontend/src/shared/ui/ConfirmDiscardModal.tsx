export function ConfirmDiscardModal({
  saving,
  onSaveRequest,
  onDiscard,
  onCancel
}: {
  saving: boolean;
  onSaveRequest: () => void;
  onDiscard: () => void;
  onCancel: () => void;
}) {
  return (
    <div
      className="modal-overlay"
      onClick={(event) => {
        event.stopPropagation();
        onCancel();
      }}
    >
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Cambios sin guardar</h2>
        </div>
        <p className="confirm-summary-subtitle">Tienes cambios sin guardar en esta historia clinica. Deseas guardar los cambios?</p>
        <div className="form-actions">
          <button className="btn ghost" type="button" onClick={onCancel}>
            Seguir editando
          </button>
          <button className="btn secondary" type="button" onClick={onDiscard}>
            Descartar cambios
          </button>
          <button className="btn primary" type="button" disabled={saving} onClick={onSaveRequest}>
            Guardar
          </button>
        </div>
      </div>
    </div>
  );
}

export function ConfirmSaveChangesModal({
  saving,
  onConfirm,
  onCancel
}: {
  saving: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div
      className="modal-overlay"
      onClick={(event) => {
        event.stopPropagation();
        onCancel();
      }}
    >
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Confirmar guardado</h2>
        </div>
        <p className="confirm-summary-subtitle">Estas seguro de guardar los cambios actuales?</p>
        <div className="form-actions">
          <button className="btn ghost" type="button" disabled={saving} onClick={onCancel}>
            Cancelar
          </button>
          <button className="btn primary" type="button" disabled={saving} onClick={onConfirm}>
            {saving ? "Guardando..." : "Si, guardar cambios"}
          </button>
        </div>
      </div>
    </div>
  );
}
