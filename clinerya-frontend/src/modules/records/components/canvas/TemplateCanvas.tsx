import { useRef, useState } from "react";
import { CANVAS_WIDTH, bandBoundaries, defaultSizeForType, sectionIdForY } from "@modules/records/constants/nomHistoryTemplate";
import { genFieldId, type TemplateElement } from "@modules/records/types";
import { CanvasElementCard } from "@modules/records/components/canvas/CanvasElementCard";
import { DRAG_MIME, type DragBlueprint } from "@modules/records/components/canvas/TemplatePalette";

const SNAP_THRESHOLD = 6;

interface Box {
  x: number;
  y: number;
  width: number;
  height: number;
}

interface SelectionRect {
  x0: number;
  y0: number;
  x1: number;
  y1: number;
}

function normalizeRect(rect: SelectionRect) {
  return {
    left: Math.min(rect.x0, rect.x1),
    top: Math.min(rect.y0, rect.y1),
    right: Math.max(rect.x0, rect.x1),
    bottom: Math.max(rect.y0, rect.y1)
  };
}

function intersects(rect: { left: number; top: number; right: number; bottom: number }, box: Box) {
  return box.x < rect.right && box.x + box.width > rect.left && box.y < rect.bottom && box.y + box.height > rect.top;
}

function computeSnap(box: Box, others: TemplateElement[], canvasWidth: number, canvasHeight: number) {
  const centerX = box.x + box.width / 2;
  const centerY = box.y + box.height / 2;
  const right = box.x + box.width;
  const bottom = box.y + box.height;

  const candidatesV: number[] = [0, canvasWidth / 2, canvasWidth];
  const candidatesH: number[] = [0, canvasHeight / 2, canvasHeight];
  others.forEach((el) => {
    candidatesV.push(el.x, el.x + el.width / 2, el.x + el.width);
    candidatesH.push(el.y, el.y + el.height / 2, el.y + el.height);
  });

  let snapDx = 0;
  let bestV = SNAP_THRESHOLD;
  let guideV: number | null = null;
  [box.x, centerX, right].forEach((edge) => {
    candidatesV.forEach((candidate) => {
      const dist = Math.abs(edge - candidate);
      if (dist < bestV) {
        bestV = dist;
        snapDx = candidate - edge;
        guideV = candidate;
      }
    });
  });

  let snapDy = 0;
  let bestH = SNAP_THRESHOLD;
  let guideH: number | null = null;
  [box.y, centerY, bottom].forEach((edge) => {
    candidatesH.forEach((candidate) => {
      const dist = Math.abs(edge - candidate);
      if (dist < bestH) {
        bestH = dist;
        snapDy = candidate - edge;
        guideH = candidate;
      }
    });
  });

  return { snapDx, snapDy, guideV, guideH };
}

export function TemplateCanvas({
  elements,
  canvasHeight,
  enforceBands,
  selectedIds,
  onSelectionChange,
  onElementsChange
}: {
  elements: TemplateElement[];
  canvasHeight: number;
  enforceBands: boolean;
  selectedIds: string[];
  onSelectionChange: (ids: string[]) => void;
  onElementsChange: (elements: TemplateElement[]) => void;
}) {
  const canvasRef = useRef<HTMLDivElement>(null);
  const dragStartPositionsRef = useRef<Record<string, { x: number; y: number }>>({});
  const [marquee, setMarquee] = useState<SelectionRect | null>(null);
  const [guides, setGuides] = useState<{ v: number | null; h: number | null }>({ v: null, h: null });

  const bands = enforceBands
    ? bandBoundaries()
        .filter((band) => band.top < canvasHeight)
        .map((band) => ({ ...band, bottom: Math.min(band.bottom, canvasHeight) }))
    : [];

  const handleDrop = (event: React.DragEvent) => {
    event.preventDefault();
    const raw = event.dataTransfer.getData(DRAG_MIME);
    if (!raw || !canvasRef.current) return;

    const blueprint = JSON.parse(raw) as DragBlueprint;
    const rect = canvasRef.current.getBoundingClientRect();
    const size = defaultSizeForType(blueprint.type);
    const x = Math.max(0, Math.min(CANVAS_WIDTH - size.width, event.clientX - rect.left));
    const y = Math.max(0, Math.min(canvasHeight - size.height, event.clientY - rect.top));

    const id = blueprint.sourceFieldId ?? genFieldId();
    if (elements.some((element) => element.id === id)) return;

    const newElement: TemplateElement = {
      id,
      sectionId: enforceBands ? blueprint.sectionId ?? sectionIdForY(y) : undefined,
      label: blueprint.label,
      type: blueprint.type,
      options: blueprint.options,
      columns: blueprint.columns,
      x,
      y,
      width: size.width,
      height: size.height
    };

    onElementsChange([...elements, newElement]);
  };

  const updateElement = (next: TemplateElement) => {
    const withRecalculatedSection: TemplateElement = enforceBands ? { ...next, sectionId: sectionIdForY(next.y) } : next;
    onElementsChange(elements.map((element) => (element.id === next.id ? withRecalculatedSection : element)));
  };

  const removeElement = (id: string) => {
    onElementsChange(elements.filter((element) => element.id !== id));
    if (selectedIds.includes(id)) onSelectionChange(selectedIds.filter((selectedId) => selectedId !== id));
  };

  const sectionCounts = (sectionId: string) => elements.filter((element) => element.sectionId === sectionId).length;

  const handleElementPointerDownSelect = (id: string, event: React.PointerEvent): string[] => {
    let next: string[];
    if (event.shiftKey) {
      next = selectedIds.includes(id) ? selectedIds.filter((selectedId) => selectedId !== id) : [...selectedIds, id];
    } else if (selectedIds.includes(id) && selectedIds.length > 1) {
      next = selectedIds;
    } else {
      next = [id];
    }
    onSelectionChange(next);

    const positions: Record<string, { x: number; y: number }> = {};
    next.forEach((elementId) => {
      const element = elements.find((item) => item.id === elementId);
      if (element) positions[elementId] = { x: element.x, y: element.y };
    });
    dragStartPositionsRef.current = positions;

    return next;
  };

  const handleDragBy = (ids: string[], dx: number, dy: number) => {
    const starts = dragStartPositionsRef.current;
    const relevantIds = ids.filter((id) => starts[id]);
    if (relevantIds.length === 0) return;

    let minDx = -Infinity;
    let maxDx = Infinity;
    let minDy = -Infinity;
    let maxDy = Infinity;
    relevantIds.forEach((id) => {
      const element = elements.find((item) => item.id === id);
      const start = starts[id];
      if (!element) return;
      minDx = Math.max(minDx, -start.x);
      maxDx = Math.min(maxDx, CANVAS_WIDTH - element.width - start.x);
      minDy = Math.max(minDy, -start.y);
      maxDy = Math.min(maxDy, canvasHeight - element.height - start.y);
    });

    const clampedDx = Math.min(Math.max(dx, minDx), maxDx);
    const clampedDy = Math.min(Math.max(dy, minDy), maxDy);

    const boxes = relevantIds
      .map((id) => {
        const element = elements.find((item) => item.id === id);
        const start = starts[id];
        if (!element) return null;
        return { x: start.x + clampedDx, y: start.y + clampedDy, width: element.width, height: element.height };
      })
      .filter((box): box is Box => box !== null);
    const groupBox: Box = {
      x: Math.min(...boxes.map((box) => box.x)),
      y: Math.min(...boxes.map((box) => box.y)),
      width: Math.max(...boxes.map((box) => box.x + box.width)) - Math.min(...boxes.map((box) => box.x)),
      height: Math.max(...boxes.map((box) => box.y + box.height)) - Math.min(...boxes.map((box) => box.y))
    };

    const others = elements.filter((element) => !relevantIds.includes(element.id));
    const { snapDx, snapDy, guideV, guideH } = computeSnap(groupBox, others, CANVAS_WIDTH, canvasHeight);

    const finalDx = Math.min(Math.max(clampedDx + snapDx, minDx), maxDx);
    const finalDy = Math.min(Math.max(clampedDy + snapDy, minDy), maxDy);

    setGuides({ v: guideV, h: guideH });

    onElementsChange(
      elements.map((element) => {
        if (!relevantIds.includes(element.id)) return element;
        const start = starts[element.id];
        const nextX = start.x + finalDx;
        const nextY = start.y + finalDy;
        return enforceBands
          ? { ...element, x: nextX, y: nextY, sectionId: sectionIdForY(nextY) }
          : { ...element, x: nextX, y: nextY };
      })
    );
  };

  const handleDragEnd = () => {
    setGuides({ v: null, h: null });
  };

  const handleCanvasMouseDown = (event: React.MouseEvent) => {
    if (event.target !== canvasRef.current || !canvasRef.current) return;
    const rect = canvasRef.current.getBoundingClientRect();
    const startX = event.clientX - rect.left;
    const startY = event.clientY - rect.top;
    const additive = event.shiftKey;
    let moved = false;

    setMarquee({ x0: startX, y0: startY, x1: startX, y1: startY });

    const handleMouseMove = (moveEvent: MouseEvent) => {
      const curX = Math.max(0, Math.min(CANVAS_WIDTH, moveEvent.clientX - rect.left));
      const curY = Math.max(0, Math.min(canvasHeight, moveEvent.clientY - rect.top));
      if (Math.abs(curX - startX) > 2 || Math.abs(curY - startY) > 2) moved = true;
      setMarquee({ x0: startX, y0: startY, x1: curX, y1: curY });
    };

    const handleMouseUp = () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
      setMarquee((current) => {
        if (current && moved) {
          const normalized = normalizeRect(current);
          const hitIds = elements.filter((element) => intersects(normalized, element)).map((element) => element.id);
          if (additive) {
            onSelectionChange(Array.from(new Set([...selectedIds, ...hitIds])));
          } else {
            onSelectionChange(hitIds);
          }
        } else if (!additive) {
          onSelectionChange([]);
        }
        return null;
      });
    };

    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);
  };

  const marqueeRect = marquee ? normalizeRect(marquee) : null;

  return (
    <div className="canvas-scroll">
      <div
        className="canvas-page"
        ref={canvasRef}
        style={{ width: CANVAS_WIDTH, height: canvasHeight }}
        onDragOver={(event) => event.preventDefault()}
        onDrop={handleDrop}
        onMouseDown={handleCanvasMouseDown}
      >
        {bands.map((band) => (
          <div
            key={band.sectionId}
            className={`canvas-band ${sectionCounts(band.sectionId) < band.minElements ? "under-minimum" : ""}`}
            style={{ top: band.top, height: band.bottom - band.top }}
          >
            <div className="canvas-band-header">
              <span>{band.title}</span>
              <span className="canvas-band-count">
                {sectionCounts(band.sectionId)}/{band.minElements} sugeridos
              </span>
            </div>
          </div>
        ))}

        {elements.map((element) => (
          <CanvasElementCard
            key={element.id}
            element={element}
            canvasWidth={CANVAS_WIDTH}
            canvasHeight={canvasHeight}
            selected={selectedIds.includes(element.id)}
            onPointerDownSelect={(event) => handleElementPointerDownSelect(element.id, event)}
            onDragBy={handleDragBy}
            onDragEnd={handleDragEnd}
            onChange={updateElement}
            onRemove={() => removeElement(element.id)}
          />
        ))}

        {guides.v !== null && <div className="canvas-guide canvas-guide-v" style={{ left: guides.v }} />}
        {guides.h !== null && <div className="canvas-guide canvas-guide-h" style={{ top: guides.h }} />}

        {marqueeRect && (
          <div
            className="canvas-marquee"
            style={{
              left: marqueeRect.left,
              top: marqueeRect.top,
              width: marqueeRect.right - marqueeRect.left,
              height: marqueeRect.bottom - marqueeRect.top
            }}
          />
        )}
      </div>
    </div>
  );
}
