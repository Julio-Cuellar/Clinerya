import { jsPDF } from "jspdf";
import {
  MARGIN_MM,
  PAGE_HEIGHT_MM,
  PAGE_WIDTH_MM,
  drawClinicHeaderBlock,
  loadImageAsDataUrl,
  type ClinicHeaderInfo,
  type ResolvedClinicHeaderInfo
} from "@modules/records/lib/pdfExport";
import { QUOTATION_STATUS_LABELS, type QuotationResponse } from "@modules/treatments/quotationTypes";
import type { ClinicResponse } from "@modules/clinics/types";
import type { PatientResponse } from "@modules/patients/types";

const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });

function formatClinicAddressLine(clinic: ClinicResponse): string {
  return [clinic.addressStreet, clinic.addressColonia, clinic.addressMunicipality, clinic.addressState, clinic.addressZip]
    .filter(Boolean)
    .join(", ");
}

export function clinicToHeaderInfo(clinic: ClinicResponse): ClinicHeaderInfo {
  return {
    name: clinic.name,
    legalName: clinic.legalName,
    addressLine: formatClinicAddressLine(clinic),
    phone: clinic.phone,
    email: clinic.email,
    logoUrl: clinic.logoUrl,
    cofeprisPermitNumber: clinic.cofeprisPermitNumber,
    responsibleDoctorName: clinic.responsibleDoctorName,
    responsibleDoctorProfessionalLicense: clinic.responsibleDoctorProfessionalLicense
  };
}

export async function exportQuotationToPdf(options: {
  quotation: QuotationResponse;
  patient: PatientResponse;
  clinicInfo?: ClinicHeaderInfo;
}) {
  const { quotation, patient, clinicInfo } = options;

  const resolvedClinicInfo: ResolvedClinicHeaderInfo | undefined = clinicInfo
    ? { ...clinicInfo, logoDataUrl: clinicInfo.logoUrl ? (await loadImageAsDataUrl(clinicInfo.logoUrl)) ?? undefined : undefined }
    : undefined;

  const doc = new jsPDF({ unit: "mm", format: "letter" });

  drawClinicHeaderBlock(doc, MARGIN_MM, MARGIN_MM, 95, 22, resolvedClinicInfo, "Clínica");

  doc.setTextColor(26, 26, 26);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(14);
  doc.text("Cotización de tratamiento", PAGE_WIDTH_MM - MARGIN_MM, MARGIN_MM + 6, { align: "right" });
  doc.setFont("helvetica", "normal");
  doc.setFontSize(9);
  doc.text(`Fecha: ${quotation.quotationDate}`, PAGE_WIDTH_MM - MARGIN_MM, MARGIN_MM + 12, { align: "right" });
  doc.text(`Estado: ${QUOTATION_STATUS_LABELS[quotation.status]}`, PAGE_WIDTH_MM - MARGIN_MM, MARGIN_MM + 17, { align: "right" });
  if (quotation.validUntil) {
    doc.text(`Vigente hasta: ${quotation.validUntil}`, PAGE_WIDTH_MM - MARGIN_MM, MARGIN_MM + 22, { align: "right" });
  }

  let cursorY = MARGIN_MM + 32;
  doc.setDrawColor(200);
  doc.line(MARGIN_MM, cursorY, PAGE_WIDTH_MM - MARGIN_MM, cursorY);
  cursorY += 6;

  doc.setFont("helvetica", "bold");
  doc.setFontSize(10);
  const patientName = [patient.firstName, patient.lastNamePaterno, patient.lastNameMaterno].filter(Boolean).join(" ");
  doc.text(`Paciente: ${patientName}`, MARGIN_MM, cursorY);
  cursorY += 8;

  const columns = ["Descripción", "Diente", "M. Obra", "Materiales", "Desc. %", "Subtotal"];
  const colWidths = [70, 14, 26, 26, 14, 38];
  const tableX = MARGIN_MM;
  const rowHeight = 7;

  doc.setFontSize(8.5);
  doc.setDrawColor(180);

  const drawRow = (cells: string[], bold: boolean) => {
    doc.setFont("helvetica", bold ? "bold" : "normal");
    let cellX = tableX;
    cells.forEach((cell, index) => {
      const width = colWidths[index];
      doc.rect(cellX, cursorY, width, rowHeight);
      const lines = doc.splitTextToSize(cell, width - 2);
      doc.text(lines[0] ?? "", cellX + 1.5, cursorY + 4.5);
      cellX += width;
    });
    cursorY += rowHeight;
  };

  drawRow(columns, true);
  quotation.items.forEach((item) => {
    if (cursorY > PAGE_HEIGHT_MM - MARGIN_MM - 30) return;
    drawRow(
      [
        item.description,
        item.toothNumber ? String(item.toothNumber) : "—",
        currencyFormatter.format(item.laborCharge),
        currencyFormatter.format(item.materialsTotal),
        `${item.discountPercentage ?? 0}%`,
        currencyFormatter.format(item.subtotal)
      ],
      false
    );
  });

  cursorY += 4;
  doc.setFont("helvetica", "bold");
  doc.setFontSize(11);
  doc.text(`Total general: ${currencyFormatter.format(quotation.grandTotal)}`, PAGE_WIDTH_MM - MARGIN_MM, cursorY, {
    align: "right"
  });

  if (quotation.notes) {
    cursorY += 10;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(9);
    doc.text("Notas:", MARGIN_MM, cursorY);
    cursorY += 5;
    doc.setFont("helvetica", "normal");
    const noteLines = doc.splitTextToSize(quotation.notes, PAGE_WIDTH_MM - MARGIN_MM * 2);
    doc.text(noteLines, MARGIN_MM, cursorY);
  }

  const safeName = patientName.trim().replace(/[^a-z0-9áéíóúñü_\- ]/gi, "").replace(/\s+/g, "_") || "paciente";
  doc.save(`Cotizacion_${safeName}_${quotation.quotationDate}.pdf`);
}
