import { useMemo, useState } from "react";
import { IconArrowsSort, IconPencil, IconSortAscending, IconSortDescending, IconTrash } from "@tabler/icons-react";
import type { ClinicStaffResponse, StaffPayrollLineResponse } from "@shared/api/api";
import { formatCurrency, staffMemberName, sumPayrollLines } from "@modules/staff/lib/payroll";

type SortKey = "name" | "baseSalary" | "commissionAmount" | "bonusAmount" | "deductionAmount" | "netAmount";
type SortDir = "asc" | "desc";

const columns: { key: SortKey; label: string; numeric: boolean }[] = [
  { key: "name", label: "Empleado", numeric: false },
  { key: "baseSalary", label: "Sueldo", numeric: true },
  { key: "commissionAmount", label: "Comision", numeric: true },
  { key: "bonusAmount", label: "Bono", numeric: true },
  { key: "deductionAmount", label: "Deduccion", numeric: true },
  { key: "netAmount", label: "Neto", numeric: true }
];

/** Read-only table of the selected period's lines, with employee filter + sort + totals. */
export function PayrollLinesTable({
  staff,
  lines,
  loading,
  canModify,
  onEditLine,
  onDeleteLine
}: {
  staff: ClinicStaffResponse[];
  lines: StaffPayrollLineResponse[];
  loading: boolean;
  canModify: boolean;
  onEditLine: (line: StaffPayrollLineResponse) => void;
  onDeleteLine: (line: StaffPayrollLineResponse) => void;
}) {
  const [filter, setFilter] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("name");
  const [sortDir, setSortDir] = useState<SortDir>("asc");

  const visibleLines = useMemo(() => {
    const needle = filter.trim().toLowerCase();
    const filtered = needle
      ? lines.filter((line) => staffMemberName(staff, line.staffId).toLowerCase().includes(needle))
      : lines;
    const factor = sortDir === "asc" ? 1 : -1;
    return [...filtered].sort((a, b) => {
      if (sortKey === "name") {
        return staffMemberName(staff, a.staffId).localeCompare(staffMemberName(staff, b.staffId), "es") * factor;
      }
      return ((a[sortKey] ?? 0) - (b[sortKey] ?? 0)) * factor;
    });
  }, [lines, staff, filter, sortKey, sortDir]);

  const totals = sumPayrollLines(visibleLines);
  const isFiltered = filter.trim().length > 0;

  const toggleSort = (key: SortKey) => {
    if (key === sortKey) {
      setSortDir((current) => (current === "asc" ? "desc" : "asc"));
    } else {
      setSortKey(key);
      setSortDir(key === "name" ? "asc" : "desc");
    }
  };

  const sortIcon = (key: SortKey) => {
    if (key !== sortKey) return <IconArrowsSort size={13} aria-hidden="true" />;
    return sortDir === "asc"
      ? <IconSortAscending size={13} aria-hidden="true" />
      : <IconSortDescending size={13} aria-hidden="true" />;
  };

  return (
    <>
      <div className="payroll-lines-toolbar">
        <input
          type="search"
          placeholder="Filtrar por empleado"
          value={filter}
          onChange={(event) => setFilter(event.target.value)}
        />
        {isFiltered && (
          <button className="btn ghost" type="button" onClick={() => setFilter("")}>Limpiar</button>
        )}
      </div>
      <div className="table-wrapper">
        <table className="data-table no-row-click">
          <thead>
            <tr>
              {columns.map((column) => (
                <th key={column.key}>
                  <button
                    type="button"
                    className={`payroll-sort-header${column.numeric ? " is-numeric" : ""}`}
                    onClick={() => toggleSort(column.key)}
                    aria-label={`Ordenar por ${column.label}`}
                  >
                    {column.label}
                    {sortIcon(column.key)}
                  </button>
                </th>
              ))}
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr><td colSpan={7}>Cargando nomina...</td></tr>
            )}
            {!loading && lines.length === 0 && (
              <tr>
                <td colSpan={7}>
                  <div className="empty-table-state">
                    <strong>Sin lineas de nomina</strong>
                    <span>Agrega empleados al periodo seleccionado.</span>
                  </div>
                </td>
              </tr>
            )}
            {!loading && lines.length > 0 && visibleLines.length === 0 && (
              <tr>
                <td colSpan={7}>
                  <div className="empty-table-state">
                    <strong>Ningun empleado coincide</strong>
                    <span>Ajusta el filtro para ver las lineas del periodo.</span>
                  </div>
                </td>
              </tr>
            )}
            {visibleLines.map((line) => (
              <tr key={line.id}>
                <td>{staffMemberName(staff, line.staffId)}</td>
                <td>{formatCurrency(line.baseSalary)}</td>
                <td>{formatCurrency(line.commissionAmount)}</td>
                <td>{formatCurrency(line.bonusAmount)}</td>
                <td>{formatCurrency(line.deductionAmount)}</td>
                <td>{formatCurrency(line.netAmount)}</td>
                <td className="table-actions">
                  <button
                    className="icon-btn"
                    type="button"
                    title="Editar linea"
                    aria-label={`Editar linea de ${staffMemberName(staff, line.staffId)}`}
                    disabled={!canModify}
                    onClick={() => onEditLine(line)}
                  >
                    <IconPencil size={16} aria-hidden="true" />
                  </button>
                  <button
                    className="icon-btn destructive"
                    type="button"
                    title="Eliminar linea"
                    aria-label={`Eliminar linea de ${staffMemberName(staff, line.staffId)}`}
                    disabled={!canModify}
                    onClick={() => {
                      if (window.confirm(`¿Eliminar la linea de ${staffMemberName(staff, line.staffId)} de este periodo?`)) {
                        onDeleteLine(line);
                      }
                    }}
                  >
                    <IconTrash size={16} aria-hidden="true" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
          {!loading && visibleLines.length > 0 && (
            <tfoot>
              <tr>
                <th>Totales ({isFiltered ? `${visibleLines.length} de ${lines.length}` : lines.length})</th>
                <td>{formatCurrency(totals.baseSalary)}</td>
                <td>{formatCurrency(totals.commissionAmount)}</td>
                <td>{formatCurrency(totals.bonusAmount)}</td>
                <td>{formatCurrency(totals.deductionAmount)}</td>
                <td>{formatCurrency(totals.netAmount)}</td>
                <td aria-hidden="true" />
              </tr>
            </tfoot>
          )}
        </table>
      </div>
    </>
  );
}
