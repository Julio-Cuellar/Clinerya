import { useEffect, useRef, useState } from "react";
import { IconX } from "@tabler/icons-react";
import { icd10Api } from "@shared/api/api";
import type { DiagnosisEntryInput, Icd10CodeResponse } from "@modules/records/types";

export function Icd10Autocomplete({
  value,
  onChange,
  disabled = false
}: {
  value: DiagnosisEntryInput[];
  onChange: (next: DiagnosisEntryInput[]) => void;
  disabled?: boolean;
}) {
  const [term, setTerm] = useState("");
  const [results, setResults] = useState<Icd10CodeResponse[]>([]);
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (term.trim().length < 2) {
      setResults([]);
      return;
    }
    let cancelled = false;
    const timer = setTimeout(() => {
      icd10Api
        .search(term.trim())
        .then((found) => {
          if (!cancelled) {
            setResults(found);
            setOpen(true);
          }
        })
        .catch(() => {
          if (!cancelled) setResults([]);
        });
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [term]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const addCode = (code: Icd10CodeResponse) => {
    if (value.some((entry) => entry.icd10Code === code.code)) {
      setTerm("");
      setOpen(false);
      return;
    }
    const hasPrimary = value.some((entry) => entry.kind === "PRIMARY");
    onChange([...value, { icd10Code: code.code, kind: hasPrimary ? "SECONDARY" : "PRIMARY" }]);
    setTerm("");
    setResults([]);
    setOpen(false);
  };

  const removeCode = (icd10Code: string) => {
    onChange(value.filter((entry) => entry.icd10Code !== icd10Code));
  };

  const makePrimary = (icd10Code: string) => {
    onChange(value.map((entry) => ({ ...entry, kind: entry.icd10Code === icd10Code ? "PRIMARY" : "SECONDARY" })));
  };

  return (
    <div className="icd10-autocomplete" ref={containerRef}>
      {value.length > 0 && (
        <div className="diagnosis-chip-list">
          {value.map((entry) => (
            <span key={entry.icd10Code} className={`diagnosis-chip ${entry.kind === "PRIMARY" ? "primary" : ""}`}>
              <strong>{entry.icd10Code}</strong>
              {entry.kind === "PRIMARY" ? (
                <span className="diagnosis-chip-label">Principal</span>
              ) : (
                !disabled && (
                  <button type="button" className="diagnosis-chip-make-primary" onClick={() => makePrimary(entry.icd10Code)}>
                    Hacer principal
                  </button>
                )
              )}
              {!disabled && (
                <button
                  type="button"
                  className="icon-btn diagnosis-chip-remove"
                  aria-label={`Quitar ${entry.icd10Code}`}
                  onClick={() => removeCode(entry.icd10Code)}
                >
                  <IconX size={12} />
                </button>
              )}
            </span>
          ))}
        </div>
      )}

      {!disabled && (
        <div className="icd10-autocomplete-input">
          <input
            type="text"
            placeholder="Buscar diagnóstico CIE-10 por código o descripción…"
            value={term}
            onChange={(event) => setTerm(event.target.value)}
            onFocus={() => results.length > 0 && setOpen(true)}
          />
          {open && results.length > 0 && (
            <ul className="icd10-autocomplete-results">
              {results.map((code) => (
                <li key={code.code}>
                  <button type="button" onClick={() => addCode(code)}>
                    <strong>{code.code}</strong> — {code.description}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
