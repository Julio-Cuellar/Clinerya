import { useEffect, useRef, useState } from "react";

const CANVAS_WIDTH = 480;
const CANVAS_HEIGHT = 180;
const STROKE_WIDTH = 2.4;

type SignaturePoint = { x: number; y: number };
type SignaturePath = SignaturePoint[];

type ParsedSignature = {
  image: string;
  backgroundImage: string;
  paths: SignaturePath[];
};

type StoredSignature = {
  version: 1;
  image: string;
  backgroundImage?: string;
  paths: SignaturePath[];
};

const signatureDraftCache = new Map<string, ParsedSignature>();

function clonePaths(paths: SignaturePath[]) {
  return paths.map((path) => path.map((point) => ({ ...point })));
}

function cloneSignature(signature: ParsedSignature): ParsedSignature {
  return {
    image: signature.image,
    backgroundImage: signature.backgroundImage,
    paths: clonePaths(signature.paths)
  };
}

function isSignaturePath(value: unknown): value is SignaturePath {
  return (
    Array.isArray(value) &&
    value.every(
      (point) =>
        point &&
        typeof point === "object" &&
        typeof (point as SignaturePoint).x === "number" &&
        typeof (point as SignaturePoint).y === "number"
    )
  );
}

function parseSignatureValue(value: string | undefined): ParsedSignature {
  if (!value) return { image: "", backgroundImage: "", paths: [] };
  if (value.startsWith("data:image/")) return { image: value, backgroundImage: value, paths: [] };

  try {
    const parsed = JSON.parse(value) as Partial<StoredSignature>;
    const paths = Array.isArray(parsed.paths) ? parsed.paths.filter(isSignaturePath) : [];
    const image = typeof parsed.image === "string" ? parsed.image : "";
    const backgroundImage = typeof parsed.backgroundImage === "string" ? parsed.backgroundImage : paths.length > 0 ? "" : image;
    return { image, backgroundImage, paths };
  } catch {
    return { image: value, backgroundImage: value, paths: [] };
  }
}

function serializeSignatureValue(image: string, backgroundImage: string, paths: SignaturePath[]) {
  return JSON.stringify({
    version: 1,
    image,
    backgroundImage: backgroundImage || undefined,
    paths
  } satisfies StoredSignature);
}

function loadImage(src: string): Promise<HTMLImageElement | null> {
  if (!src) return Promise.resolve(null);
  return new Promise((resolve) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => resolve(null);
    image.src = src;
  });
}

function drawPaths(ctx: CanvasRenderingContext2D, paths: SignaturePath[]) {
  ctx.strokeStyle = "#1a1a1a";
  ctx.fillStyle = "#1a1a1a";
  ctx.lineWidth = STROKE_WIDTH;
  ctx.lineCap = "round";
  ctx.lineJoin = "round";

  paths.forEach((path) => {
    if (path.length === 0) return;
    if (path.length === 1) {
      ctx.beginPath();
      ctx.arc(path[0].x, path[0].y, STROKE_WIDTH, 0, Math.PI * 2);
      ctx.fill();
      return;
    }

    ctx.beginPath();
    ctx.moveTo(path[0].x, path[0].y);
    path.slice(1).forEach((point) => ctx.lineTo(point.x, point.y));
    ctx.stroke();
  });
}

async function buildSignatureDataUrl(backgroundImage: string, backgroundImageElement: HTMLImageElement | null, paths: SignaturePath[]) {
  const canvas = document.createElement("canvas");
  canvas.width = CANVAS_WIDTH;
  canvas.height = CANVAS_HEIGHT;
  const ctx = canvas.getContext("2d");
  if (!ctx) return "";

  if (backgroundImage) {
    const image =
      backgroundImageElement?.complete && backgroundImageElement.naturalWidth > 0
        ? backgroundImageElement
        : await loadImage(backgroundImage);
    if (image) ctx.drawImage(image, 0, 0, canvas.width, canvas.height);
  }

  drawPaths(ctx, paths);
  return canvas.toDataURL("image/png");
}

function pathPoints(path: SignaturePath) {
  return path.map((point) => `${point.x},${point.y}`).join(" ");
}

export function getSignatureImageValue(value: string | undefined) {
  return parseSignatureValue(value).image;
}

export function SignaturePad({
  label,
  signerName,
  value,
  onChange,
  storageKey,
  disabled = false
}: {
  label: string;
  signerName?: string;
  value: string | undefined;
  onChange: (next: string) => void;
  storageKey?: string;
  disabled?: boolean;
}) {
  const cacheKey = storageKey ?? `signature:${label}:${signerName ?? ""}`;
  const initial = signatureDraftCache.has(cacheKey)
    ? cloneSignature(signatureDraftCache.get(cacheKey)!)
    : parseSignatureValue(value);
  const surfaceRef = useRef<HTMLDivElement>(null);
  const backgroundImageRef = useRef<HTMLImageElement>(null);
  const drawingRef = useRef(false);
  const suppressClickRef = useRef(false);
  const activePointerRef = useRef<number | null>(null);
  const imageRef = useRef(initial.image);
  const backgroundImageValueRef = useRef(initial.backgroundImage);
  const pathsRef = useRef<SignaturePath[]>(initial.paths);
  const hasLocalEditRef = useRef(false);
  const renderVersionRef = useRef(0);
  const removeWindowListenersRef = useRef<(() => void) | null>(null);

  const [image, setImageState] = useState(initial.image);
  const [backgroundImage, setBackgroundImageState] = useState(initial.backgroundImage);
  const [paths, setPathsState] = useState<SignaturePath[]>(initial.paths);
  const [hasStroke, setHasStroke] = useState(Boolean(initial.image || initial.backgroundImage || initial.paths.length));
  const canClear = !disabled && hasStroke;
  const visibleImage = paths.length > 0 ? backgroundImage : image || backgroundImage;

  const setImage = (next: string) => {
    imageRef.current = next;
    setImageState(next);
    signatureDraftCache.set(cacheKey, cloneSignature({ image: next, backgroundImage: backgroundImageValueRef.current, paths: pathsRef.current }));
  };

  const setBackgroundImage = (next: string) => {
    backgroundImageValueRef.current = next;
    setBackgroundImageState(next);
    signatureDraftCache.set(cacheKey, cloneSignature({ image: imageRef.current, backgroundImage: next, paths: pathsRef.current }));
  };

  const setPaths = (next: SignaturePath[]) => {
    pathsRef.current = next;
    setPathsState(next);
    signatureDraftCache.set(cacheKey, cloneSignature({ image: imageRef.current, backgroundImage: backgroundImageValueRef.current, paths: next }));
  };

  const emitSignatureValue = (nextPaths: SignaturePath[], nextImage = imageRef.current) => {
    onChange(serializeSignatureValue(nextImage, backgroundImageValueRef.current, nextPaths));
  };

  const applyParsedSignature = (parsed: ParsedSignature) => {
    setImage(parsed.image);
    setBackgroundImage(parsed.backgroundImage);
    setPaths(parsed.paths);
    setHasStroke(Boolean(parsed.image || parsed.backgroundImage || parsed.paths.length));
  };

  const removeWindowListeners = () => {
    removeWindowListenersRef.current?.();
    removeWindowListenersRef.current = null;
  };

  useEffect(() => () => removeWindowListeners(), []);

  useEffect(() => {
    if (hasLocalEditRef.current) return;
    if (drawingRef.current) return;
    renderVersionRef.current += 1;
    applyParsedSignature(parseSignatureValue(value));
  }, [value]);

  const pointFromClient = (clientX: number, clientY: number): SignaturePoint | null => {
    const surface = surfaceRef.current;
    if (!surface) return null;
    const rect = surface.getBoundingClientRect();
    return {
      x: Math.min(Math.max(((clientX - rect.left) / rect.width) * CANVAS_WIDTH, 0), CANVAS_WIDTH),
      y: Math.min(Math.max(((clientY - rect.top) / rect.height) * CANVAS_HEIGHT, 0), CANVAS_HEIGHT)
    };
  };

  const appendPoint = (point: SignaturePoint) => {
    const currentPaths = pathsRef.current;
    const activePath = currentPaths[currentPaths.length - 1] ?? [];
    const previousPoint = activePath[activePath.length - 1];
    if (previousPoint && Math.hypot(point.x - previousPoint.x, point.y - previousPoint.y) < 0.7) return;

    const nextPaths = [...currentPaths];
    nextPaths[nextPaths.length - 1] = [...activePath, point];
    setPaths(nextPaths);
    setHasStroke(true);
    emitSignatureValue(nextPaths);
  };

  const continueStroke = (clientX: number, clientY: number, pointerId: number) => {
    if (disabled || !drawingRef.current || activePointerRef.current !== pointerId) return;
    const point = pointFromClient(clientX, clientY);
    if (!point) return;
    appendPoint(point);
  };

  const finishCurrentStroke = () => {
    if (!drawingRef.current) return;
    // El navegador sintetiza el evento "click" segun la posicion real del
    // cursor al soltarlo, sin respetar setPointerCapture. Si el trazo termina
    // sobre el boton "Limpiar firma" (el lienzo es compacto), ese click
    // fantasma llegaria a handleClear y borraria lo recien dibujado.
    suppressClickRef.current = true;

    const surface = surfaceRef.current;
    if (surface && activePointerRef.current !== null && surface.hasPointerCapture(activePointerRef.current)) {
      surface.releasePointerCapture(activePointerRef.current);
    }

    removeWindowListeners();
    drawingRef.current = false;
    activePointerRef.current = null;

    const currentBackgroundImage = backgroundImageValueRef.current;
    const currentPaths = pathsRef.current.map((path) => [...path]);
    const hasDrawnPoints = currentPaths.some((path) => path.length > 0);
    if (!currentBackgroundImage && !hasDrawnPoints) return;

    const renderVersion = renderVersionRef.current + 1;
    renderVersionRef.current = renderVersion;

    void buildSignatureDataUrl(currentBackgroundImage, backgroundImageRef.current, currentPaths).then((nextImage) => {
      if (!nextImage || renderVersionRef.current !== renderVersion) return;
      setImage(nextImage);
      setHasStroke(true);
      onChange(serializeSignatureValue(nextImage, currentBackgroundImage, currentPaths));
    });
  };

  const addWindowListeners = () => {
    removeWindowListeners();
    const handlePointerMove = (event: PointerEvent) => {
      event.preventDefault();
      continueStroke(event.clientX, event.clientY, event.pointerId);
    };
    const handleEnd = (event: PointerEvent) => {
      event.preventDefault();
      finishCurrentStroke();
    };

    window.addEventListener("pointermove", handlePointerMove, { passive: false });
    window.addEventListener("pointerup", handleEnd, { passive: false });
    window.addEventListener("pointercancel", handleEnd, { passive: false });
    removeWindowListenersRef.current = () => {
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", handleEnd);
      window.removeEventListener("pointercancel", handleEnd);
    };
  };

  const handlePointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    suppressClickRef.current = false;
    if (disabled || drawingRef.current) return;

    const point = pointFromClient(event.clientX, event.clientY);
    if (!point) return;

    try {
      surfaceRef.current?.setPointerCapture(event.pointerId);
    } catch {
      // algunos navegadores pueden rechazar la captura si el puntero ya no esta activo
    }

    activePointerRef.current = event.pointerId;
    drawingRef.current = true;
    hasLocalEditRef.current = true;
    renderVersionRef.current += 1;

    if (pathsRef.current.length === 0 && imageRef.current && !backgroundImageValueRef.current) {
      setBackgroundImage(imageRef.current);
    }

    const nextPaths = [...pathsRef.current, [point]];
    setPaths(nextPaths);
    setHasStroke(true);
    emitSignatureValue(nextPaths);
    addWindowListeners();
  };

  const handleClear = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.stopPropagation();
    if (suppressClickRef.current) {
      suppressClickRef.current = false;
      return;
    }
    if (disabled) return;

    renderVersionRef.current += 1;
    hasLocalEditRef.current = true;
    removeWindowListeners();
    drawingRef.current = false;
    activePointerRef.current = null;
    setImage("");
    setBackgroundImage("");
    setPaths([]);
    signatureDraftCache.set(cacheKey, { image: "", backgroundImage: "", paths: [] });
    setHasStroke(false);
    onChange("");
  };

  return (
    <div className="signature-pad">
      <div
        ref={surfaceRef}
        className="signature-pad-canvas-wrap"
        aria-disabled={disabled}
        onPointerDown={handlePointerDown}
      >
        {visibleImage && (
          <img
            ref={backgroundImageRef}
            className="signature-pad-image"
            src={visibleImage}
            alt=""
            aria-hidden="true"
          />
        )}
        <svg className="signature-pad-overlay" viewBox={`0 0 ${CANVAS_WIDTH} ${CANVAS_HEIGHT}`} aria-hidden="true">
          {paths.map((path, index) =>
            path.length === 1 ? (
              <circle key={index} cx={path[0].x} cy={path[0].y} r={STROKE_WIDTH} className="signature-pad-dot" />
            ) : (
              <polyline key={index} points={pathPoints(path)} className="signature-pad-stroke" />
            )
          )}
        </svg>
      </div>
      <div className="signature-pad-footer">
        <span className="signature-pad-line">{signerName || label}</span>
        <button className="btn ghost signature-pad-clear" type="button" onClick={handleClear} disabled={!canClear}>
          Limpiar firma
        </button>
      </div>
    </div>
  );
}
