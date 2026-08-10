import { useState } from "react";
import { IconX } from "@tabler/icons-react";
import type { PdfExportRange } from "@modules/records/lib/pdfExport";

export function ExportPdfModal({
  totalPages,
  currentPage,
  onCancel,
  onConfirm
}: {
  totalPages: number;
  currentPage: number;
  onCancel: () => void;
  onConfirm: (range: PdfExportRange) => void;
}) {
  const [mode, setMode] = useState<"all" | "current" | "range">("all");
  const [from, setFrom] = useState(currentPage);
  const [to, setTo] = useState(totalPages);

  const handleConfirm = () => {
    if (mode === "all") onConfirm({});
    else if (mode === "current") onConfirm({ from: currentPage, to: currentPage });
    else onConfirm({ from: Math.min(from, to), to: Math.max(from, to) });
  };

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Exportar a PDF</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onCancel}>
            <IconX size={18} />
          </button>
        </div>

        <div className="export-pdf-options">
          <label className="export-pdf-option">
            <input type="radio" checked={mode === "all"} onChange={() => setMode("all")} />
            Todas las páginas ({totalPages})
          </label>
          <label className="export-pdf-option">
            <input type="radio" checked={mode === "current"} onChange={() => setMode("current")} />
            Solo la página actual ({currentPage})
          </label>
          <label className="export-pdf-option">
            <input type="radio" checked={mode === "range"} onChange={() => setMode("range")} />
            Rango de páginas
          </label>
          {mode === "range" && (
            <div className="export-pdf-range">
              <label>
                De
                <input
                  type="number"
                  min={1}
                  max={totalPages}
                  value={from}
                  onChange={(event) => setFrom(Number(event.target.value) || 1)}
                />
              </label>
              <label>
                A
                <input
                  type="number"
                  min={1}
                  max={totalPages}
                  value={to}
                  onChange={(event) => setTo(Number(event.target.value) || totalPages)}
                />
              </label>
            </div>
          )}
        </div>

        <div className="form-actions">
          <button className="btn secondary" type="button" onClick={onCancel}>
            Cancelar
          </button>
          <button className="btn primary" type="button" onClick={handleConfirm}>
            Exportar
          </button>
        </div>
      </div>
    </div>
  );
}
