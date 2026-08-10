import { useState } from "react";
import {
  LOWER_ARCH,
  SURFACE_CONDITION_COLORS,
  SURFACE_CONDITION_LABELS,
  SURFACE_CONDITION_ORDER,
  SURFACE_LABELS,
  UPPER_ARCH,
  WHOLE_CONDITION_BADGES,
  WHOLE_CONDITION_LABELS,
  WHOLE_CONDITION_ORDER,
  emptyTooth,
  parseOdontogram,
  serializeOdontogram,
  type ToothRecord,
  type ToothSurfaceCode
} from "@modules/records/constants/odontogram";

const TOOTH_SIZE = 34;

interface MenuState {
  tooth: number;
  surface?: ToothSurfaceCode;
  align: "left" | "right";
}

function ToothSvg({ record }: { record: ToothRecord }) {
  const fillFor = (surface: ToothSurfaceCode) => SURFACE_CONDITION_COLORS[record.surfaces[surface] ?? "sano"];
  const badge = WHOLE_CONDITION_BADGES[record.whole];
  const surfaceProps = (surface: ToothSurfaceCode) => ({
    "data-surface": surface,
    fill: fillFor(surface),
    pointerEvents: "auto" as const
  });

  return (
    <svg width={TOOTH_SIZE} height={TOOTH_SIZE} viewBox="0 0 40 40" className="odontogram-tooth-svg">
      <polygon points="0,0 40,0 20,20" stroke="var(--color-border)" {...surfaceProps("V")}>
        <title>Vestibular</title>
      </polygon>
      <polygon points="40,0 40,40 20,20" stroke="var(--color-border)" {...surfaceProps("D")}>
        <title>Distal</title>
      </polygon>
      <polygon points="40,40 0,40 20,20" stroke="var(--color-border)" {...surfaceProps("L")}>
        <title>Palatino/Lingual</title>
      </polygon>
      <polygon points="0,40 0,0 20,20" stroke="var(--color-border)" {...surfaceProps("M")}>
        <title>Mesial</title>
      </polygon>
      <rect x={14} y={14} width={12} height={12} stroke="var(--color-border)" {...surfaceProps("O")}>
        <title>Oclusal/Incisal</title>
      </rect>
      <rect x={0.5} y={0.5} width={39} height={39} fill="none" stroke="var(--color-border)" pointerEvents="none" />
      {record.whole === "ausente" && (
        <g pointerEvents="none" stroke="#6b7280" strokeWidth={3}>
          <line x1={2} y1={2} x2={38} y2={38} />
          <line x1={38} y1={2} x2={2} y2={38} />
        </g>
      )}
      {record.whole === "extraccion_indicada" && (
        <line x1={2} y1={2} x2={38} y2={38} stroke="#D64545" strokeWidth={3} pointerEvents="none" />
      )}
      {record.whole === "corona" && (
        <rect x={1} y={1} width={38} height={38} fill="none" stroke="#C9971F" strokeWidth={3} pointerEvents="none" />
      )}
      {record.whole === "fracturado" && (
        <polyline points="2,20 12,10 20,26 28,10 38,20" fill="none" stroke="#E08A2C" strokeWidth={2} pointerEvents="none" />
      )}
      {badge && (
        <g pointerEvents="none">
          <circle cx={33} cy={7} r={6.5} fill={badge.color} />
          <text x={33} y={9.5} textAnchor="middle" fontSize={6.5} fill="#fff" fontWeight={700}>
            {badge.text}
          </text>
        </g>
      )}
    </svg>
  );
}

function ConditionMenu({
  menu,
  onChooseSurface,
  onChooseWhole,
  onClose
}: {
  menu: MenuState;
  onChooseSurface: (code: (typeof SURFACE_CONDITION_ORDER)[number]) => void;
  onChooseWhole: (code: (typeof WHOLE_CONDITION_ORDER)[number]) => void;
  onClose: () => void;
}) {
  return (
    <div className={`odontogram-menu odontogram-menu-${menu.align}`} onClick={(event) => event.stopPropagation()}>
      <div className="odontogram-menu-header">
        <div className="odontogram-menu-title">Pieza {menu.tooth}</div>
        <button type="button" className="odontogram-menu-close" aria-label="Cerrar menu" onClick={onClose}>
          x
        </button>
      </div>

      {menu.surface && (
        <>
          <div className="odontogram-menu-section">Cara: {SURFACE_LABELS[menu.surface]}</div>
          {SURFACE_CONDITION_ORDER.map((code) => (
            <button type="button" key={code} className="odontogram-menu-item" onClick={() => onChooseSurface(code)}>
              <i className="odontogram-legend-swatch" style={{ background: SURFACE_CONDITION_COLORS[code] }} />
              {SURFACE_CONDITION_LABELS[code]}
            </button>
          ))}
        </>
      )}

      <div className="odontogram-menu-section">Pieza completa</div>
      {WHOLE_CONDITION_ORDER.map((code) => {
        const badge = WHOLE_CONDITION_BADGES[code];
        return (
          <button type="button" key={code} className="odontogram-menu-item" onClick={() => onChooseWhole(code)}>
            {badge ? (
              <i className="odontogram-legend-badge" style={{ background: badge.color }}>
                {badge.text}
              </i>
            ) : code === "sano" ? (
              <i className="odontogram-legend-swatch" />
            ) : (
              <i className={`odontogram-legend-symbol odontogram-legend-symbol-${code}`} />
            )}
            {WHOLE_CONDITION_LABELS[code]}
          </button>
        );
      })}
    </div>
  );
}

export function OdontogramField({
  value,
  onChange,
  readOnly = false
}: {
  value: string | undefined;
  onChange: (next: string) => void;
  readOnly?: boolean;
}) {
  const data = parseOdontogram(value);
  const [menu, setMenu] = useState<MenuState | null>(null);

  const updateTooth = (tooth: number, updater: (rec: ToothRecord) => ToothRecord) => {
    const key = String(tooth);
    const current = data.teeth[key] ?? emptyTooth();
    onChange(serializeOdontogram({ teeth: { ...data.teeth, [key]: updater(current) } }));
  };

  const openMenu = (tooth: number, align: "left" | "right", surface: ToothSurfaceCode | undefined, event: React.MouseEvent) => {
    event.preventDefault();
    event.stopPropagation();
    setMenu({ tooth, surface, align });
  };

  const renderArch = (teeth: number[], label: string) => (
    <div className="odontogram-arch">
      <div className="odontogram-arch-label">{label}</div>
      <div className="odontogram-arch-teeth">
        {teeth.map((tooth, index) => {
          const record = data.teeth[String(tooth)] ?? emptyTooth();
          const isActive = menu?.tooth === tooth;
          const align = index >= teeth.length / 2 ? "right" : "left";

          const handleToothClick = (event: React.MouseEvent) => {
            if (readOnly) return;
            const target = event.target;
            if (!(target instanceof Element)) return;

            const surfaceTarget = target.closest("[data-surface]") as HTMLElement | null;
            if (surfaceTarget) {
              const surface = surfaceTarget.getAttribute("data-surface") as ToothSurfaceCode | null;
              if (surface) {
                openMenu(tooth, align, surface, event);
              }
              return;
            }

            const toothNumberTarget = target.closest("[data-tooth-number='true']");
            if (toothNumberTarget) {
              openMenu(tooth, align, undefined, event);
            }
          };

          return (
            <div
              className={`odontogram-tooth ${isActive ? "odontogram-tooth-active" : ""}`}
              key={tooth}
              onClick={handleToothClick}
            >
              <ToothSvg record={record} />
              <button
                type="button"
                data-tooth-number="true"
                className={`odontogram-tooth-number ${record.whole !== "sano" ? "marked" : ""}`}
                title="Abrir opciones de la pieza"
              >
                {tooth}
              </button>

              {isActive && (
                <ConditionMenu
                  menu={menu}
                  onClose={() => setMenu(null)}
                  onChooseSurface={(code) => {
                    updateTooth(tooth, (rec) => ({
                      ...rec,
                      surfaces: { ...rec.surfaces, [menu.surface as ToothSurfaceCode]: code }
                    }));
                    setMenu(null);
                  }}
                  onChooseWhole={(code) => {
                    updateTooth(tooth, (rec) => ({ ...rec, whole: code }));
                    setMenu(null);
                  }}
                />
              )}
            </div>
          );
        })}
      </div>
    </div>
  );

  return (
    <div className="odontogram-field" onClick={() => {
      if (menu) setMenu(null);
    }}>
      {renderArch(UPPER_ARCH, "Arcada superior")}
      {renderArch(LOWER_ARCH, "Arcada inferior")}
      <div className="odontogram-legend">
        {SURFACE_CONDITION_ORDER.filter((code) => code !== "sano").map((code) => (
          <span className="odontogram-legend-item" key={code}>
            <i className="odontogram-legend-swatch" style={{ background: SURFACE_CONDITION_COLORS[code] }} />
            {SURFACE_CONDITION_LABELS[code]}
          </span>
        ))}
        {WHOLE_CONDITION_ORDER.filter((code) => code !== "sano").map((code) => {
          const badge = WHOLE_CONDITION_BADGES[code];
          return (
            <span className="odontogram-legend-item" key={code}>
              {badge ? (
                <i className="odontogram-legend-badge" style={{ background: badge.color }}>
                  {badge.text}
                </i>
              ) : (
                <i className={`odontogram-legend-symbol odontogram-legend-symbol-${code}`} />
              )}
              {WHOLE_CONDITION_LABELS[code]}
            </span>
          );
        })}
      </div>
      <p className="odontogram-hint">
        Haz clic en cualquier parte de un diente (una cara o el numero) para abrir la lista completa de condiciones y elegir la
        que corresponda.
      </p>
    </div>
  );
}
