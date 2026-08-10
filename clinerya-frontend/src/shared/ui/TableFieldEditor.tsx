import { IconPlus, IconTrash } from "@tabler/icons-react";
import { parseTableRows, serializeTableRows } from "@modules/records/types";

export function TableFieldEditor({
  columns,
  value,
  onChange,
  readOnly = false,
  readOnlyColumns = [],
  disableRowActions = false
}: {
  columns: string[];
  value: string | undefined;
  onChange: (next: string) => void;
  readOnly?: boolean;
  readOnlyColumns?: number[];
  disableRowActions?: boolean;
}) {
  const rows = parseTableRows(value, columns.length);
  const readOnlyColumnSet = new Set(readOnlyColumns);

  const updateCell = (rowIndex: number, colIndex: number, cellValue: string) => {
    if (readOnly || readOnlyColumnSet.has(colIndex)) return;
    const next = rows.map((row, r) => (r === rowIndex ? row.map((cell, c) => (c === colIndex ? cellValue : cell)) : row));
    onChange(serializeTableRows(next));
  };

  const addRow = () => {
    if (readOnly || disableRowActions) return;
    onChange(serializeTableRows([...rows, new Array(columns.length).fill("")]));
  };

  const removeRow = (rowIndex: number) => {
    if (readOnly || disableRowActions) return;
    const next = rows.filter((_, r) => r !== rowIndex);
    onChange(serializeTableRows(next.length > 0 ? next : [new Array(columns.length).fill("")]));
  };

  return (
    <div className="table-field">
      <table className="table-field-grid">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column}>{column}</th>
            ))}
            {!readOnly && !disableRowActions && <th aria-label="Acciones" />}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr key={rowIndex}>
              {columns.map((_, colIndex) => (
                <td key={colIndex}>
                  <input
                    value={row[colIndex] ?? ""}
                    disabled={readOnly || readOnlyColumnSet.has(colIndex)}
                    onChange={(event) => updateCell(rowIndex, colIndex, event.target.value)}
                  />
                </td>
              ))}
              {!readOnly && !disableRowActions && (
                <td>
                  <button type="button" className="icon-btn" aria-label="Eliminar fila" onClick={() => removeRow(rowIndex)}>
                    <IconTrash size={14} />
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
      {!readOnly && !disableRowActions && (
        <button type="button" className="btn ghost table-field-add-row" onClick={addRow}>
          <IconPlus size={14} aria-hidden="true" />
          Agregar fila
        </button>
      )}
    </div>
  );
}
