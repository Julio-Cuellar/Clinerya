import { useEffect, useState } from "react";
import { IconDeviceFloppy, IconDental, IconFileText, IconFileTypePdf, IconPlus, IconTrash, IconX } from "@tabler/icons-react";
import { buildNomOdontologyStarterPages } from "@modules/records/constants/nomOdontologyTemplate";
import { buildNomStarterPages, checkCompliance, ensureConsentPage, sectionIdForY, CANVAS_PAGE_HEIGHT, CANVAS_WIDTH } from "@modules/records/constants/nomHistoryTemplate";
import { exportPagesToPdf, type PdfExportRange } from "@modules/records/lib/pdfExport";
import { getFriendlyError, historyTemplatesApi } from "@shared/api/api";
import {
  genFieldId,
  genPageId,
  parseSchema,
  serializeSchema,
  type HistoryTemplateResponse,
  type TemplateElement,
  type TemplateKind,
  type TemplatePage,
  type TemplateSchema
} from "@modules/records/types";
import { ComplianceOverrideModal } from "@modules/agenda/components/ComplianceOverrideModal";
import { ExportPdfModal } from "@shared/ui/ExportPdfModal";
import { TemplateCanvas } from "@modules/records/components/canvas/TemplateCanvas";
import { TemplatePalette } from "@modules/records/components/canvas/TemplatePalette";
import { TypographyToolbar } from "@modules/records/components/canvas/TypographyToolbar";
import { ClinicalMappingToolbar } from "@modules/records/components/canvas/ClinicalMappingToolbar";

export function TemplateModal({
  clinicId,
  template,
  onClose,
  onSaved
}: {
  clinicId: string;
  template?: HistoryTemplateResponse;
  onClose: () => void;
  onSaved: (template: HistoryTemplateResponse) => void;
}) {
  const parsedInitialSchema = template ? parseSchema(template.schemaJson) : null;
  const initialSchema = parsedInitialSchema?.kind === "historia_clinica" ? { ...parsedInitialSchema, pages: ensureConsentPage(parsedInitialSchema.pages) } : parsedInitialSchema;
  const [kind, setKind] = useState<TemplateKind | null>(initialSchema?.kind ?? null);
  const [name, setName] = useState(template?.name ?? "");
  const [description, setDescription] = useState(template?.description ?? "");
  const [pages, setPages] = useState<TemplatePage[]>(initialSchema?.pages ?? []);
  const [pageIndex, setPageIndex] = useState(0);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [compliance, setCompliance] = useState(initialSchema?.compliance);
  const [showOverride, setShowOverride] = useState<string[] | null>(null);
  const [showExport, setShowExport] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const currentPage = pages[pageIndex];
  const enforceBands = kind === "historia_clinica" && pageIndex === 0;

  const setCurrentPageElements = (elements: TemplateElement[]) => {
    setPages((prev) => prev.map((page, index) => (index === pageIndex ? { ...page, elements } : page)));
  };

  const changePage = (index: number) => {
    setPageIndex(index);
    setSelectedIds([]);
  };

  const addPage = () => {
    const created: TemplatePage = { id: genPageId(), elements: [], canvasHeight: CANVAS_PAGE_HEIGHT };
    setPages((prev) => [...prev, created]);
    changePage(pages.length);
  };

  const removePage = (index: number) => {
    if (pages.length <= 1) return;
    setPages((prev) => prev.filter((_, i) => i !== index));
    setSelectedIds([]);
    setPageIndex((prev) => Math.max(0, prev >= index ? prev - 1 : prev));
  };

  // El toolbar de tipografía opera sobre el último elemento seleccionado como referencia, pero el
  // cambio (p. ej. tamaño de letra) se transmite a todos los elementos seleccionados: solo se
  // aplican las propiedades que realmente cambiaron para no pisar la posición/tamaño de cada uno.
  const updateSelectedElement = (next: TemplateElement) => {
    const reference = currentPage?.elements.find((element) => element.id === next.id);
    if (!reference) return;
    const changedKeys = (Object.keys(next) as Array<keyof TemplateElement>).filter((key) => next[key] !== reference[key]);
    setCurrentPageElements(
      (currentPage?.elements ?? []).map((element) => {
        if (!selectedIds.includes(element.id)) return element;
        const patch: Partial<TemplateElement> = {};
        changedKeys.forEach((key) => {
          (patch as Record<string, unknown>)[key] = next[key];
        });
        return { ...element, ...patch };
      })
    );
  };

  // El mapeo clínico es propio de un solo campo (los índices de columna dependen de su tabla),
  // así que se reemplaza el elemento completo sin propagar a la multiselección ni hacer diff de claves.
  const replaceSelectedElement = (next: TemplateElement) => {
    setCurrentPageElements((currentPage?.elements ?? []).map((element) => (element.id === next.id ? next : element)));
  };

  const selectedElement = currentPage?.elements.find((element) => element.id === selectedIds[selectedIds.length - 1]) ?? null;

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (showOverride || showExport) return;
      const target = event.target as HTMLElement | null;
      const isTyping =
        target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement || Boolean(target?.isContentEditable);

      if (event.key === "Escape") {
        setSelectedIds([]);
        return;
      }
      if (isTyping) return;

      const elements = currentPage?.elements ?? [];
      const pageCanvasHeight = currentPage?.canvasHeight ?? CANVAS_PAGE_HEIGHT;

      if ((event.key === "Delete" || event.key === "Backspace") && selectedIds.length > 0) {
        event.preventDefault();
        setCurrentPageElements(elements.filter((element) => !selectedIds.includes(element.id)));
        setSelectedIds([]);
        return;
      }

      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "a") {
        event.preventDefault();
        setSelectedIds(elements.map((element) => element.id));
        return;
      }

      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "d" && selectedIds.length > 0) {
        event.preventDefault();
        const duplicates = elements
          .filter((element) => selectedIds.includes(element.id))
          .map((element) => {
            const x = Math.min(CANVAS_WIDTH - element.width, element.x + 16);
            const y = Math.min(pageCanvasHeight - element.height, element.y + 16);
            return {
              ...element,
              id: genFieldId(),
              sectionId: enforceBands ? sectionIdForY(y) : undefined,
              x,
              y
            };
          });
        setCurrentPageElements([...elements, ...duplicates]);
        setSelectedIds(duplicates.map((element) => element.id));
        return;
      }

      if (event.key.startsWith("Arrow") && selectedIds.length > 0) {
        event.preventDefault();
        const step = event.shiftKey ? 10 : 1;
        const dx = event.key === "ArrowLeft" ? -step : event.key === "ArrowRight" ? step : 0;
        const dy = event.key === "ArrowUp" ? -step : event.key === "ArrowDown" ? step : 0;
        setCurrentPageElements(
          elements.map((element) => {
            if (!selectedIds.includes(element.id)) return element;
            const nextX = Math.max(0, Math.min(CANVAS_WIDTH - element.width, element.x + dx));
            const nextY = Math.max(0, Math.min(pageCanvasHeight - element.height, element.y + dy));
            return enforceBands
              ? { ...element, x: nextX, y: nextY, sectionId: sectionIdForY(nextY) }
              : { ...element, x: nextX, y: nextY };
          })
        );
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedIds, currentPage, enforceBands, showOverride, showExport]);

  const persist = async (schemaOverride?: Partial<TemplateSchema>) => {
    if (!kind) return;
    setLoading(true);
    setError("");
    try {
      const schema: TemplateSchema = { kind, pages, compliance, ...schemaOverride };
      const schemaJson = serializeSchema(schema);

      const saved = template
        ? await historyTemplatesApi.update(clinicId, template.id, {
            name,
            description: description || undefined,
            schemaJson,
            active: template.active
          })
        : await historyTemplatesApi.create(clinicId, { name, description: description || undefined, schemaJson });

      onSaved(saved);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  const handleSave = () => {
    if (!name.trim()) {
      setError("El nombre de la plantilla es obligatorio.");
      return;
    }
    if (pages.every((page) => page.elements.length === 0)) {
      setError("Agrega al menos un elemento al lienzo.");
      return;
    }

    if (kind === "historia_clinica" && !compliance?.overridden) {
      const result = checkCompliance(pages.flatMap((page) => page.elements));
      if (!result.compliant) {
        setShowOverride(result.missingSections);
        return;
      }
    }

    persist();
  };

  const confirmOverride = () => {
    if (!showOverride) return;
    const nextCompliance = {
      overridden: true,
      overriddenAt: new Date().toISOString(),
      missingSections: showOverride
    };
    setCompliance(nextCompliance);
    setShowOverride(null);
    persist({ compliance: nextCompliance });
  };

  const handleExport = (range: PdfExportRange) => {
    setShowExport(false);
    exportPagesToPdf({ title: name || "Plantilla", pages, canvasWidthPx: CANVAS_WIDTH, range });
  };

  if (!kind) {
    return (
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-card" onClick={(event) => event.stopPropagation()}>
          <div className="panel-heading">
            <h2>Nueva plantilla</h2>
            <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
              <IconX size={18} />
            </button>
          </div>
          <p className="confirm-summary-subtitle">Elige el tipo de plantilla que quieres crear:</p>
          <div className="template-kind-options">
            <button
              className="template-kind-card"
              type="button"
              onClick={() => {
                setKind("historia_clinica");
                setPages(buildNomStarterPages());
              }}
            >
              <IconFileText size={28} aria-hidden="true" />
              <strong>Historia clínica (NOM-004)</strong>
              <span>Incluye las secciones y campos sugeridos por la norma, listos para editar.</span>
            </button>
            <button
              className="template-kind-card"
              type="button"
              onClick={() => {
                setKind("historia_clinica");
                setPages(buildNomOdontologyStarterPages());
              }}
            >
              <IconDental size={28} aria-hidden="true" />
              <strong>Historia clínica odontológica (NOM-013)</strong>
              <span>Formato odontologico del cliente con antecedentes, sistemas, odontograma, consentimientos y contrato.</span>
            </button>
            <button
              className="template-kind-card"
              type="button"
              onClick={() => {
                setKind("complementario");
                setPages([{ id: genPageId(), elements: [], canvasHeight: CANVAS_PAGE_HEIGHT }]);
              }}
            >
              <IconFileText size={28} aria-hidden="true" />
              <strong>Historia clínica personalizada</strong>
              <span>Lienzo libre para documentos adicionales (odontograma, nutrición, etc.).</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="canvas-editor-overlay">
      <header className="canvas-editor-header">
        <div className="canvas-editor-header-fields">
          <input
            className="canvas-editor-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Nombre de la plantilla"
          />
          <input
            className="canvas-editor-description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Descripción (opcional)"
          />
        </div>
        <div className="canvas-editor-header-actions">
          {error && <span className="alert error">{error}</span>}
          <button className="btn ghost" type="button" onClick={() => setShowExport(true)} disabled={loading}>
            <IconFileTypePdf size={18} aria-hidden="true" />
            Exportar PDF
          </button>
          <button className="btn secondary" type="button" onClick={onClose} disabled={loading}>
            Cancelar
          </button>
          <button className="btn primary" type="button" onClick={handleSave} disabled={loading}>
            <IconDeviceFloppy size={18} aria-hidden="true" />
            {loading ? "Guardando" : "Guardar"}
          </button>
        </div>
      </header>

      <div className="canvas-page-tabs">
        {pages.map((page, index) => (
          <div key={page.id} className={`canvas-page-tab ${index === pageIndex ? "active" : ""}`}>
            <button type="button" onClick={() => changePage(index)}>
              Página {index + 1}
            </button>
            {pages.length > 1 && (
              <button
                type="button"
                className="canvas-page-tab-remove"
                aria-label={`Eliminar página ${index + 1}`}
                onClick={() => removePage(index)}
              >
                <IconTrash size={12} />
              </button>
            )}
          </div>
        ))}
        <button type="button" className="canvas-page-tab-add" onClick={addPage}>
          <IconPlus size={14} aria-hidden="true" />
          Página
        </button>
      </div>

      {selectedElement && (
        <>
          <TypographyToolbar element={selectedElement} onChange={updateSelectedElement} />
          <ClinicalMappingToolbar element={selectedElement} onChange={replaceSelectedElement} />
        </>
      )}

      <div className="canvas-editor-body">
        <TemplatePalette enforceBands={enforceBands} existingElements={currentPage?.elements ?? []} />
        <TemplateCanvas
          elements={currentPage?.elements ?? []}
          canvasHeight={currentPage?.canvasHeight ?? 400}
          enforceBands={enforceBands}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onElementsChange={setCurrentPageElements}
        />
      </div>

      {showOverride && (
        <ComplianceOverrideModal
          missingSections={showOverride}
          onCancel={() => setShowOverride(null)}
          onConfirm={confirmOverride}
        />
      )}

      {showExport && (
        <ExportPdfModal
          totalPages={pages.length}
          currentPage={pageIndex + 1}
          onCancel={() => setShowExport(false)}
          onConfirm={handleExport}
        />
      )}
    </div>
  );
}
