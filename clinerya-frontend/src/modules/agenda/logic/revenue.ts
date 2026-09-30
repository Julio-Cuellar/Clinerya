/** Ingreso estimado de la agenda (plan v2): presupuestos ligados + precio fijo de las citas con servicio. */

interface RevenueAppointment {
  status: string;
  quotationId?: string;
  quotationItemId?: string;
  quotationItemIds?: string[];
  servicePricing?: string | null;
  servicePrice?: number | null;
}

interface RevenueQuotation {
  grandTotal: number;
  items: Array<{ id: string; subtotal: number }>;
}

const INACTIVE = new Set(["CANCELLED", "NO_SHOW"]);

/**
 * Solo citas vigentes. Si la cita tiene presupuesto, manda el presupuesto (sin doble conteo); si no, su
 * precio fijo. Las de precio variable no suman: se cuentan aparte como "por definir".
 */
export function estimatedRevenue(
  appointments: RevenueAppointment[],
  quotations: Map<string, RevenueQuotation>
): { total: number; toDefine: number } {
  let total = 0;
  let toDefine = 0;
  for (const appointment of appointments) {
    if (INACTIVE.has(appointment.status)) continue;
    if (appointment.quotationId) {
      total += quotationAmount(appointment, quotations.get(appointment.quotationId));
    } else if (appointment.servicePricing === "FIXED" && typeof appointment.servicePrice === "number") {
      total += appointment.servicePrice;
    } else if (appointment.servicePricing === "VARIES_BY_PATIENT") {
      toDefine += 1;
    }
  }
  return { total, toDefine };
}

function quotationAmount(appointment: RevenueAppointment, quotation?: RevenueQuotation): number {
  if (!quotation) return 0;
  if (appointment.quotationItemIds?.length) {
    return quotation.items.filter((item) => appointment.quotationItemIds!.includes(item.id))
      .reduce((sum, item) => sum + item.subtotal, 0);
  }
  if (appointment.quotationItemId) {
    return quotation.items.find((item) => item.id === appointment.quotationItemId)?.subtotal ?? 0;
  }
  return quotation.grandTotal;
}

/** Hora de fin (input datetime-local "AAAA-MM-DDTHH:mm") al sumar la duracion del servicio. */
export function endForService(start: string, durationMinutes: number): string {
  if (!start) return "";
  const [datePart, timePart] = start.split("T");
  const [year, month, day] = datePart.split("-").map(Number);
  const [hour, minute] = timePart.split(":").map(Number);
  const end = new Date(year, month - 1, day, hour, minute + durationMinutes);
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}T${pad(end.getHours())}:${pad(end.getMinutes())}`;
}
