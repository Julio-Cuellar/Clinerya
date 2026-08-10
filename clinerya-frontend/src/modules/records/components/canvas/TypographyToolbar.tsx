import { IconAlignCenter, IconAlignLeft, IconAlignRight, IconBold, IconColorSwatch } from "@tabler/icons-react";
import {
  DEFAULT_FONT_FAMILY,
  DEFAULT_FONT_SIZE,
  DEFAULT_TEXT_ALIGN,
  DEFAULT_TEXT_COLOR,
  FONT_FAMILY_OPTIONS,
  FONT_SIZE_OPTIONS,
  type TemplateElement,
  type TemplateTextAlign
} from "@modules/records/types";

export function TypographyToolbar({
  element,
  onChange
}: {
  element: TemplateElement;
  onChange: (next: TemplateElement) => void;
}) {
  const align = element.align ?? DEFAULT_TEXT_ALIGN;

  return (
    <div className="typography-toolbar">
      <select
        value={element.fontFamily ?? DEFAULT_FONT_FAMILY}
        onChange={(event) => onChange({ ...element, fontFamily: event.target.value as TemplateElement["fontFamily"] })}
        aria-label="Fuente"
      >
        {FONT_FAMILY_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      <select
        value={element.fontSize ?? DEFAULT_FONT_SIZE}
        onChange={(event) => onChange({ ...element, fontSize: Number(event.target.value) })}
        aria-label="Tamaño de letra"
      >
        {FONT_SIZE_OPTIONS.map((size) => (
          <option key={size} value={size}>
            {size}px
          </option>
        ))}
      </select>

      <button
        type="button"
        className={`typography-toggle ${element.bold ? "active" : ""}`}
        aria-label="Negrita"
        aria-pressed={Boolean(element.bold)}
        onClick={() => onChange({ ...element, bold: !element.bold })}
      >
        <IconBold size={16} />
      </button>

      <div className="typography-align-group">
        {(
          [
            { value: "left", Icon: IconAlignLeft, label: "Alinear a la izquierda" },
            { value: "center", Icon: IconAlignCenter, label: "Centrar" },
            { value: "right", Icon: IconAlignRight, label: "Alinear a la derecha" }
          ] as Array<{ value: TemplateTextAlign; Icon: typeof IconAlignLeft; label: string }>
        ).map(({ value, Icon, label }) => (
          <button
            key={value}
            type="button"
            className={`typography-toggle ${align === value ? "active" : ""}`}
            aria-label={label}
            aria-pressed={align === value}
            onClick={() => onChange({ ...element, align: value })}
          >
            <Icon size={16} />
          </button>
        ))}
      </div>

      <input
        type="color"
        className="typography-color"
        aria-label="Color de texto"
        title="Color de texto"
        value={element.color ?? DEFAULT_TEXT_COLOR}
        onChange={(event) => onChange({ ...element, color: event.target.value })}
      />

      <input
        type="color"
        className="typography-color"
        aria-label="Color de fondo"
        title="Color de fondo"
        value={element.backgroundColor ?? "#ffffff"}
        onChange={(event) => onChange({ ...element, backgroundColor: event.target.value })}
      />

      {element.backgroundColor && (
        <button
          type="button"
          className="typography-toggle"
          aria-label="Quitar color de fondo"
          title="Quitar color de fondo"
          onClick={() => onChange({ ...element, backgroundColor: undefined })}
        >
          <IconColorSwatch size={16} />
        </button>
      )}
    </div>
  );
}
