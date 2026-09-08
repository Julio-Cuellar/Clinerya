import { useState } from "react";

const groupFormatter = new Intl.NumberFormat("es-MX", {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
  useGrouping: true
});

/**
 * Text input for money amounts: shows thousands separators while idle, switches
 * to a plain editable number on focus, and reports the parsed value (or null).
 */
export function CurrencyInput({
  value,
  onValueChange,
  placeholder = "0.00",
  disabled = false,
  ariaLabel
}: {
  value: number | null;
  onValueChange: (value: number | null) => void;
  placeholder?: string;
  disabled?: boolean;
  ariaLabel?: string;
}) {
  const [focused, setFocused] = useState(false);
  const [draft, setDraft] = useState("");

  const display = focused
    ? draft
    : value === null || Number.isNaN(value)
      ? ""
      : groupFormatter.format(value);

  return (
    <input
      type="text"
      inputMode="decimal"
      aria-label={ariaLabel}
      placeholder={placeholder}
      disabled={disabled}
      value={display}
      onFocus={() => {
        setFocused(true);
        setDraft(value === null || Number.isNaN(value) ? "" : String(value));
      }}
      onBlur={() => setFocused(false)}
      onChange={(event) => {
        const cleaned = event.target.value.replace(/[^0-9.]/g, "").replace(/(\.\d*)\./g, "$1");
        setDraft(cleaned);
        onValueChange(cleaned === "" || cleaned === "." ? null : Number(cleaned));
      }}
    />
  );
}
