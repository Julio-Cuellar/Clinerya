import { jsPDF } from "jspdf";
import { CONSENT_SECTION_ID, NOM_SECTIONS } from "@modules/records/constants/nomHistoryTemplate";
import { APP_INFO_CONSENT_FIELD_IDS, ODONTOLOGY_FIELD_IDS } from "@modules/records/constants/nomOdontologyTemplate";
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
  parseOdontogram
} from "@modules/records/constants/odontogram";
import {
  DEFAULT_FONT_FAMILY,
  DEFAULT_FONT_SIZE,
  DEFAULT_TEXT_ALIGN,
  DEFAULT_TEXT_COLOR,
  fontPdfFamily,
  parseAttachments,
  parseTableRows,
  type TemplateElement,
  type TemplatePage
} from "@modules/records/types";

export const PAGE_WIDTH_MM = 216;
export const PAGE_HEIGHT_MM = 279;
export const MARGIN_MM = 14;
const CONTENT_TOP_MM = 22;

export interface ClinicHeaderInfo {
  name: string;
  legalName?: string;
  addressLine?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  cofeprisPermitNumber?: string;
  responsibleDoctorName?: string;
  responsibleDoctorProfessionalLicense?: string;
}

export interface ResolvedClinicHeaderInfo extends ClinicHeaderInfo {
  logoDataUrl?: string;
}

export function loadImageAsDataUrl(url: string): Promise<string | null> {
  if (url.startsWith("data:")) return Promise.resolve(url);
  return new Promise((resolve) => {
    const image = new Image();
    image.crossOrigin = "anonymous";
    image.onload = () => {
      try {
        const canvas = document.createElement("canvas");
        canvas.width = image.naturalWidth;
        canvas.height = image.naturalHeight;
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          resolve(null);
          return;
        }
        ctx.drawImage(image, 0, 0);
        resolve(canvas.toDataURL("image/png"));
      } catch {
        resolve(null);
      }
    };
    image.onerror = () => resolve(null);
    image.src = url;
  });
}

function pxToMm(px: number, canvasWidthPx: number) {
  const usableWidth = PAGE_WIDTH_MM - MARGIN_MM * 2;
  return (px / canvasWidthPx) * usableWidth;
}

export function hexToRgb(hex: string): [number, number, number] {
  const match = /^#?([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$/i.exec(hex);
  if (!match) return [26, 26, 26];
  return [parseInt(match[1], 16), parseInt(match[2], 16), parseInt(match[3], 16)];
}

function drawElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, value: string | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(20, pxToMm(element.width, canvasWidthPx));
  const heightMm = Math.max(8, pxToMm(element.height, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  const fontFamily = fontPdfFamily(element.fontFamily ?? DEFAULT_FONT_FAMILY);
  const fontSize = element.fontSize ?? DEFAULT_FONT_SIZE;
  const align = element.align ?? DEFAULT_TEXT_ALIGN;
  const [r, g, b] = hexToRgb(element.color ?? DEFAULT_TEXT_COLOR);
  const alignX = align === "left" ? x : align === "center" ? x + widthMm / 2 : x + widthMm;

  if (element.backgroundColor) {
    const [bgR, bgG, bgB] = hexToRgb(element.backgroundColor);
    doc.setFillColor(bgR, bgG, bgB);
    doc.rect(x, top, widthMm, heightMm, "F");
  }

  doc.setTextColor(r, g, b);
  doc.setFont(fontFamily, "bold");
  doc.setFontSize(Math.max(7, fontSize * 0.7));
  doc.text(element.label, alignX, top + 4, { align });

  doc.setFont(fontFamily, element.bold ? "bold" : "normal");
  doc.setFontSize(fontSize);
  const text = value && value.trim() ? value : "—";
  const lines = doc.splitTextToSize(text, widthMm);
  doc.text(lines, alignX, top + 8.5, { align });
}

function drawTableElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, value: string | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(20, pxToMm(element.width, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  const columns = element.columns ?? [];
  if (columns.length === 0) return;

  const fontFamily = fontPdfFamily(element.fontFamily ?? DEFAULT_FONT_FAMILY);
  const [r, g, b] = hexToRgb(element.color ?? DEFAULT_TEXT_COLOR);

  doc.setTextColor(r, g, b);
  doc.setFont(fontFamily, "bold");
  doc.setFontSize(Math.max(7, (element.fontSize ?? DEFAULT_FONT_SIZE) * 0.7));
  doc.text(element.label, x, top, { align: "left" });

  const rows = parseTableRows(value, columns.length);
  const colWidth = widthMm / columns.length;
  const rowHeight = 6;
  let cursorY = top + 4;

  doc.setFontSize(8);
  doc.setDrawColor(180);

  const drawRow = (cells: string[], bold: boolean) => {
    doc.setFont(fontFamily, bold ? "bold" : "normal");
    cells.forEach((cell, index) => {
      const cellX = x + index * colWidth;
      doc.rect(cellX, cursorY, colWidth, rowHeight);
      const lines = doc.splitTextToSize(cell || "", colWidth - 2);
      doc.text(lines[0] ?? "", cellX + 1, cursorY + 4);
    });
    cursorY += rowHeight;
  };

  drawRow(columns, true);
  rows.forEach((row) => {
    if (cursorY > PAGE_HEIGHT_MM - MARGIN_MM) return;
    drawRow(columns.map((_, index) => row[index] ?? ""), false);
  });
}

function drawFileElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, value: string | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(20, pxToMm(element.width, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  const fontFamily = fontPdfFamily(element.fontFamily ?? DEFAULT_FONT_FAMILY);
  const [r, g, b] = hexToRgb(element.color ?? DEFAULT_TEXT_COLOR);

  doc.setTextColor(r, g, b);
  doc.setFont(fontFamily, "bold");
  doc.setFontSize(Math.max(7, (element.fontSize ?? DEFAULT_FONT_SIZE) * 0.7));
  doc.text(element.label, x, top, { align: "left" });

  const attachments = parseAttachments(value);
  doc.setFont(fontFamily, "normal");
  doc.setFontSize(9);
  let cursorY = top + 5;
  if (attachments.length === 0) {
    doc.text("Sin archivos adjuntos.", x, cursorY);
    return;
  }
  attachments.forEach((attachment) => {
    if (cursorY > PAGE_HEIGHT_MM - MARGIN_MM) return;
    doc.text(`• ${attachment.originalFilename}`, x, cursorY);
    cursorY += 5;
  });
}

function drawOdontogramLegend(doc: jsPDF, x: number, startY: number, widthMm: number) {
  const surfaceItems = SURFACE_CONDITION_ORDER.filter((code) => code !== "sano").map((code) => ({
    kind: "surface" as const,
    code,
    label: SURFACE_CONDITION_LABELS[code],
    color: SURFACE_CONDITION_COLORS[code]
  }));
  const wholeItems = WHOLE_CONDITION_ORDER.filter((code) => code !== "sano").map((code) => ({
    kind: "whole" as const,
    code,
    label: WHOLE_CONDITION_LABELS[code],
    badge: WHOLE_CONDITION_BADGES[code]
  }));

  const items = [...surfaceItems, ...wholeItems];
  if (items.length === 0) return 0;

  doc.setTextColor(70, 70, 70);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(8.5);
  doc.text("Simbologia", x, startY);

  const rowTop = startY + 3.5;
  const swatch = 2.8;
  const gapX = 4.5;
  const gapY = 4.6;
  const textGap = 1.8;
  const maxItemWidth = 42;
  let cursorX = x;
  let cursorY = rowTop;
  const maxRight = x + widthMm;

  const drawSurfaceLegendItem = (item: { label: string; color: string }) => {
    doc.setDrawColor(180);
    if (item.color === "transparent") {
      doc.setFillColor(255, 255, 255);
      doc.rect(cursorX, cursorY, swatch, swatch, "S");
    } else {
      const [r, g, b] = hexToRgb(item.color);
      doc.setFillColor(r, g, b);
      doc.rect(cursorX, cursorY, swatch, swatch, "F");
    }
    doc.setTextColor(90, 90, 90);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(7);
    doc.text(item.label, cursorX + swatch + textGap, cursorY + 2.2);
  };

  const drawWholeLegendItem = (item: { code: string; label: string; badge?: { text: string; color: string } }) => {
    const centerY = cursorY + 1.4;
    doc.setDrawColor(180);
    if (item.badge) {
      const [r, g, b] = hexToRgb(item.badge.color);
      doc.setFillColor(r, g, b);
      doc.circle(cursorX + swatch / 2, centerY, swatch / 2, "F");
      doc.setTextColor(255, 255, 255);
      doc.setFont("helvetica", "bold");
      doc.setFontSize(5.5);
      doc.text(item.badge.text, cursorX + swatch / 2, centerY + 0.8, { align: "center" });
    } else if (item.code === "ausente") {
      doc.setLineWidth(0.4);
      doc.line(cursorX, cursorY, cursorX + swatch, cursorY + swatch);
      doc.line(cursorX + swatch, cursorY, cursorX, cursorY + swatch);
    } else if (item.code === "extraccion_indicada") {
      doc.setDrawColor(214, 69, 69);
      doc.setLineWidth(0.45);
      doc.line(cursorX, cursorY, cursorX + swatch, cursorY + swatch);
    } else if (item.code === "corona") {
      doc.setDrawColor(201, 151, 31);
      doc.setLineWidth(0.45);
      doc.rect(cursorX, cursorY, swatch, swatch, "S");
    } else if (item.code === "fracturado") {
      doc.setDrawColor(224, 138, 44);
      doc.setLineWidth(0.45);
      doc.line(cursorX, cursorY + swatch * 0.65, cursorX + swatch * 0.35, cursorY + swatch * 0.2);
      doc.line(cursorX + swatch * 0.35, cursorY + swatch * 0.2, cursorX + swatch * 0.65, cursorY + swatch * 0.8);
      doc.line(cursorX + swatch * 0.65, cursorY + swatch * 0.8, cursorX + swatch, cursorY + swatch * 0.35);
    } else {
      doc.setDrawColor(150);
      doc.rect(cursorX, cursorY, swatch, swatch, "S");
    }
    doc.setTextColor(90, 90, 90);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(7);
    doc.text(item.label, cursorX + swatch + textGap, cursorY + 2.2);
  };

  items.forEach((item) => {
    const estimatedWidth = Math.min(maxItemWidth, swatch + textGap + doc.getTextWidth(item.label));
    if (cursorX > x && cursorX + estimatedWidth > maxRight) {
      cursorX = x;
      cursorY += gapY;
    }

    if (item.kind === "surface") {
      drawSurfaceLegendItem(item);
    } else {
      drawWholeLegendItem(item);
    }

    cursorX += estimatedWidth + gapX;
  });

  return cursorY + gapY - startY;
}

const TOOTH_SURFACE_ORDER = ["V", "D", "M", "L", "O"] as const;

interface OdontogramFinding {
  tooth: string;
  detail: string;
}

function hasToothFinding(record: ReturnType<typeof emptyTooth>) {
  const whole = record.whole ?? "sano";
  const surfaces = record.surfaces ?? {};
  return (
    whole !== "sano" ||
    TOOTH_SURFACE_ORDER.some((surface) => {
      const condition = surfaces[surface] ?? "sano";
      return condition !== "sano";
    })
  );
}

function buildOdontogramFindings(value: string | undefined): OdontogramFinding[] {
  const data = parseOdontogram(value);
  const orderedTeeth = [...UPPER_ARCH, ...LOWER_ARCH].map(String);
  const extraTeeth = Object.keys(data.teeth)
    .filter((tooth) => !orderedTeeth.includes(tooth))
    .sort((a, b) => Number(a) - Number(b));

  return [...orderedTeeth, ...extraTeeth].flatMap((tooth) => {
    const record = data.teeth[tooth];
    if (!record || !hasToothFinding(record)) return [];

    const parts: string[] = [];
    const whole = record.whole ?? "sano";
    if (whole !== "sano") {
      parts.push(WHOLE_CONDITION_LABELS[whole] ?? whole);
    }

    TOOTH_SURFACE_ORDER.forEach((surface) => {
      const condition = record.surfaces?.[surface] ?? "sano";
      if (condition === "sano") return;
      const conditionLabel = SURFACE_CONDITION_LABELS[condition] ?? condition;
      parts.push(`${conditionLabel} (${SURFACE_LABELS[surface]})`);
    });

    return [{ tooth, detail: parts.join("; ") }];
  });
}

function odontogramFindingsHeight(doc: jsPDF, findings: OdontogramFinding[], widthMm: number) {
  if (findings.length === 0) return 0;
  const detailWidth = widthMm - 23;
  doc.setFont("helvetica", "normal");
  doc.setFontSize(7.3);
  const rowsHeight = findings.reduce((total, finding) => {
    const lines = doc.splitTextToSize(finding.detail, detailWidth);
    return total + Math.max(6.2, lines.length * 3.3 + 2.8);
  }, 0);
  return 8 + 6.5 + rowsHeight;
}

function drawOdontogramFindings(doc: jsPDF, x: number, startY: number, widthMm: number, findings: OdontogramFinding[]) {
  if (findings.length === 0) return 0;

  const toothWidth = 18;
  const detailWidth = widthMm - toothWidth;
  let cursorY = startY;

  doc.setTextColor(70, 70, 70);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(8.5);
  doc.text("Hallazgos del odontograma", x, cursorY);
  cursorY += 3.5;

  doc.setDrawColor(210);
  doc.setFillColor(238, 242, 245);
  doc.rect(x, cursorY, widthMm, 6.5, "FD");
  doc.line(x + toothWidth, cursorY, x + toothWidth, cursorY + 6.5);

  doc.setTextColor(42, 48, 55);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(7.3);
  doc.text("Diente", x + 1.5, cursorY + 4.2);
  doc.text("Condicion registrada", x + toothWidth + 1.5, cursorY + 4.2);
  cursorY += 6.5;

  findings.forEach((finding) => {
    doc.setFont("helvetica", "normal");
    doc.setFontSize(7.3);
    const detailLines = doc.splitTextToSize(finding.detail, detailWidth - 3);
    const rowHeight = Math.max(6.2, detailLines.length * 3.3 + 2.8);

    doc.setDrawColor(210);
    doc.rect(x, cursorY, widthMm, rowHeight);
    doc.line(x + toothWidth, cursorY, x + toothWidth, cursorY + rowHeight);

    doc.setTextColor(184, 44, 44);
    doc.setFont("helvetica", "bold");
    doc.text(finding.tooth, x + toothWidth / 2, cursorY + 4.3, { align: "center" });

    doc.setTextColor(45, 52, 58);
    doc.setFont("helvetica", "normal");
    doc.text(detailLines, x + toothWidth + 1.5, cursorY + 4.2);
    cursorY += rowHeight;
  });

  return cursorY - startY;
}

function drawOdontogramElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, value: string | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(60, pxToMm(element.width, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  const fontFamily = fontPdfFamily(element.fontFamily ?? DEFAULT_FONT_FAMILY);
  const [r, g, b] = hexToRgb(element.color ?? DEFAULT_TEXT_COLOR);

  doc.setTextColor(r, g, b);
  doc.setFont(fontFamily, "bold");
  doc.setFontSize(Math.max(7, (element.fontSize ?? DEFAULT_FONT_SIZE) * 0.7));
  doc.text(element.label, x, top, { align: "left" });

  const data = parseOdontogram(value);
  const toothSize = Math.min(9, widthMm / 16);
  let cursorY = top + 4;

  const drawArch = (arch: number[]) => {
    if (cursorY + toothSize + 3 > PAGE_HEIGHT_MM - MARGIN_MM) return;

    arch.forEach((tooth, index) => {
      const tx = x + index * toothSize;
      const record = data.teeth[String(tooth)] ?? emptyTooth();
      const half = toothSize / 2;
      const cx = tx + half;
      const cy = cursorY + half;
      const corners: Record<"tl" | "tr" | "bl" | "br", [number, number]> = {
        tl: [tx, cursorY],
        tr: [tx + toothSize, cursorY],
        bl: [tx, cursorY + toothSize],
        br: [tx + toothSize, cursorY + toothSize]
      };

      doc.setDrawColor(200);
      const fillTriangle = (p1: [number, number], p2: [number, number], surface: "V" | "D" | "M" | "L") => {
        const code = record.surfaces[surface] ?? "sano";
        const colorHex = SURFACE_CONDITION_COLORS[code];
        if (colorHex === "transparent") {
          doc.triangle(p1[0], p1[1], p2[0], p2[1], cx, cy, "S");
          return;
        }
        const [fr, fg, fb] = hexToRgb(colorHex);
        doc.setFillColor(fr, fg, fb);
        doc.triangle(p1[0], p1[1], p2[0], p2[1], cx, cy, "FD");
      };

      fillTriangle(corners.tl, corners.tr, "V");
      fillTriangle(corners.tr, corners.br, "D");
      fillTriangle(corners.br, corners.bl, "L");
      fillTriangle(corners.bl, corners.tl, "M");

      const oclusalCode = record.surfaces.O ?? "sano";
      const oSize = toothSize * 0.32;
      if (oclusalCode !== "sano") {
        const [orr, og, ob] = hexToRgb(SURFACE_CONDITION_COLORS[oclusalCode]);
        doc.setFillColor(orr, og, ob);
        doc.rect(cx - oSize / 2, cy - oSize / 2, oSize, oSize, "FD");
      } else {
        doc.rect(cx - oSize / 2, cy - oSize / 2, oSize, oSize, "S");
      }

      doc.rect(tx, cursorY, toothSize, toothSize, "S");

      if (record.whole === "ausente") {
        doc.setDrawColor(107, 114, 128);
        doc.line(tx + 0.5, cursorY + 0.5, tx + toothSize - 0.5, cursorY + toothSize - 0.5);
        doc.line(tx + toothSize - 0.5, cursorY + 0.5, tx + 0.5, cursorY + toothSize - 0.5);
      } else if (record.whole === "extraccion_indicada") {
        doc.setDrawColor(214, 69, 69);
        doc.line(tx + 0.5, cursorY + 0.5, tx + toothSize - 0.5, cursorY + toothSize - 0.5);
      } else if (record.whole === "corona") {
        doc.setDrawColor(201, 151, 31);
        doc.rect(tx + 0.4, cursorY + 0.4, toothSize - 0.8, toothSize - 0.8, "S");
      } else if (record.whole === "fracturado") {
        doc.setDrawColor(224, 138, 44);
        doc.lines(
          [
            [toothSize * 0.25, -toothSize * 0.25],
            [toothSize * 0.25, toothSize * 0.4],
            [toothSize * 0.25, -toothSize * 0.4],
            [toothSize * 0.25, toothSize * 0.25]
          ],
          tx + 0.5,
          cursorY + toothSize * 0.5,
          [1, 1],
          "S"
        );
      }

      const badge = WHOLE_CONDITION_BADGES[record.whole];
      if (badge) {
        const badgeR = toothSize * 0.18;
        const badgeCx = tx + toothSize - badgeR;
        const badgeCy = cursorY + badgeR;
        const [bgR, bgG, bgB] = hexToRgb(badge.color);
        doc.setFillColor(bgR, bgG, bgB);
        doc.circle(badgeCx, badgeCy, badgeR, "F");
        doc.setTextColor(255, 255, 255);
        doc.setFont(fontFamily, "bold");
        doc.setFontSize(Math.max(3, badgeR * 1.3));
        doc.text(badge.text, badgeCx, badgeCy + badgeR * 0.35, { align: "center" });
      }

      doc.setTextColor(r, g, b);
      const marked = hasToothFinding(record);
      if (marked) {
        doc.setTextColor(184, 44, 44);
      }
      doc.setFont(fontFamily, marked ? "bold" : "normal");
      doc.setFontSize(5);
      doc.text(String(tooth), cx, cursorY + toothSize + 2.5, { align: "center" });
    });

    cursorY += toothSize + 5;
  };

  drawArch(UPPER_ARCH);
  drawArch(LOWER_ARCH);

  const usedHeight = cursorY - top;
  const legendTop = top + usedHeight + 2;
  const legendHeight = drawOdontogramLegend(doc, x, legendTop, widthMm);
  const findings = buildOdontogramFindings(value);
  drawOdontogramFindings(doc, x, legendTop + legendHeight + 3, widthMm, findings);
}

export function drawClinicHeaderBlock(
  doc: jsPDF,
  x: number,
  top: number,
  widthMm: number,
  heightMm: number,
  info: ResolvedClinicHeaderInfo | undefined,
  fallbackLabel: string
) {
  if (!info) {
    doc.setTextColor(120, 120, 120);
    doc.setFont("helvetica", "italic");
    doc.setFontSize(8);
    doc.text(fallbackLabel, x, top + 4);
    return;
  }

  const logoSize = Math.min(heightMm, 18);
  let textX = x;
  if (info.logoDataUrl) {
    try {
      doc.addImage(info.logoDataUrl, "PNG", x, top, logoSize, logoSize);
      textX = x + logoSize + 3;
    } catch {
      // logotipo no disponible (formato no soportado o error de carga); se omite sin interrumpir el PDF
    }
  }

  const textWidth = Math.max(20, widthMm - (textX - x));
  const lines = [info.legalName || info.name, info.addressLine, info.phone ? `Tel. ${info.phone}` : undefined, info.email]
    .filter((line): line is string => Boolean(line));

  doc.setTextColor(26, 26, 26);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(9);
  doc.text(doc.splitTextToSize(lines[0] ?? "", textWidth), textX, top + 4);

  doc.setFont("helvetica", "normal");
  doc.setFontSize(7.5);
  let cursorY = top + 8.5;
  lines.slice(1).forEach((line) => {
    const wrapped = doc.splitTextToSize(line, textWidth);
    doc.text(wrapped, textX, cursorY);
    cursorY += 3.6 * wrapped.length;
  });
}

function drawClinicHeaderElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, info: ResolvedClinicHeaderInfo | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(30, pxToMm(element.width, canvasWidthPx));
  const heightMm = Math.max(15, pxToMm(element.height, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  drawClinicHeaderBlock(doc, x, top, widthMm, heightMm, info, element.label);
}

function drawSignatureElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, value: string | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(30, pxToMm(element.width, canvasWidthPx));
  const heightMm = Math.max(20, pxToMm(element.height, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  const lineY = top + heightMm - 8;
  const signatureImage = getSignatureImageValue(value);
  if (signatureImage) {
    try {
      doc.addImage(signatureImage, "PNG", x, top, widthMm, heightMm - 12);
    } catch {
      // firma no disponible (dato inválido); se deja el recuadro en blanco para firma en tinta
    }
  }

  doc.setDrawColor(120, 120, 120);
  doc.line(x, lineY, x + widthMm, lineY);
  doc.setTextColor(80, 80, 80);
  doc.setFont("helvetica", "normal");
  doc.setFontSize(8);
  doc.text(element.label, x + widthMm / 2, lineY + 4, { align: "center" });
}

function getSignatureImageValue(value: string | undefined) {
  if (!value) return "";
  if (value.startsWith("data:image/")) return value;
  try {
    const parsed = JSON.parse(value) as { image?: unknown };
    return typeof parsed.image === "string" ? parsed.image : "";
  } catch {
    return "";
  }
}

type PdfRgb = [number, number, number];

const CLINICAL_CONTENT_TOP_MM = 24;
const CLINICAL_CONTENT_BOTTOM_MM = PAGE_HEIGHT_MM - 18;
const CLINICAL_CONTENT_WIDTH_MM = PAGE_WIDTH_MM - MARGIN_MM * 2;
const CLINICAL_PRIMARY: PdfRgb = [20, 105, 135];
const CLINICAL_MUTED: PdfRgb = [95, 105, 115];
const CLINICAL_BORDER: PdfRgb = [210, 218, 224];
const CLINICAL_LIGHT: PdfRgb = [244, 249, 251];

interface ClinicalRenderState {
  doc: jsPDF;
  y: number;
}

function setTextRgb(doc: jsPDF, color: PdfRgb) {
  doc.setTextColor(color[0], color[1], color[2]);
}

function setDrawRgb(doc: jsPDF, color: PdfRgb) {
  doc.setDrawColor(color[0], color[1], color[2]);
}

function setFillRgb(doc: jsPDF, color: PdfRgb) {
  doc.setFillColor(color[0], color[1], color[2]);
}

function ensureClinicalSpace(state: ClinicalRenderState, heightMm: number) {
  if (state.y + heightMm <= CLINICAL_CONTENT_BOTTOM_MM) return;
  state.doc.addPage();
  state.y = CLINICAL_CONTENT_TOP_MM;
}

function clinicalValue(raw: string | undefined) {
  return raw && raw.trim() ? raw.trim() : "No registrado";
}

function hasClinicalValue(raw: string | undefined) {
  return Boolean(raw && raw.trim());
}

function isClinicalSimpleField(element: TemplateElement) {
  return (
    element.type === "text" ||
    element.type === "textarea" ||
    element.type === "number" ||
    element.type === "date" ||
    element.type === "select"
  );
}

function measureClinicalField(doc: jsPDF, element: TemplateElement, raw: string | undefined, widthMm: number) {
  const innerWidth = Math.max(16, widthMm - 6);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(7.2);
  const labelLines = doc.splitTextToSize(element.label, innerWidth);
  doc.setFont("helvetica", "normal");
  doc.setFontSize(9.2);
  const valueLines = doc.splitTextToSize(clinicalValue(raw), innerWidth);
  const height = Math.max(14, 4 + labelLines.length * 3.2 + valueLines.length * 4.2 + 3);
  return { height, labelLines, valueLines };
}

function drawClinicalFieldBox(
  doc: jsPDF,
  element: TemplateElement,
  raw: string | undefined,
  x: number,
  y: number,
  widthMm: number,
  heightMm: number
) {
  const measured = measureClinicalField(doc, element, raw, widthMm);
  setFillRgb(doc, [255, 255, 255]);
  setDrawRgb(doc, CLINICAL_BORDER);
  doc.setLineWidth(0.25);
  doc.rect(x, y, widthMm, heightMm, "FD");

  setTextRgb(doc, CLINICAL_MUTED);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(7.2);
  doc.text(measured.labelLines, x + 3, y + 4.2);

  setTextRgb(doc, hasClinicalValue(raw) ? [28, 33, 38] : [130, 140, 148]);
  doc.setFont("helvetica", hasClinicalValue(raw) ? "normal" : "italic");
  doc.setFontSize(9.2);
  doc.text(measured.valueLines, x + 3, y + 4.4 + measured.labelLines.length * 3.2 + 3.2);
}

function drawClinicalFieldGrid(state: ClinicalRenderState, elements: TemplateElement[], answers?: Record<string, string>) {
  const doc = state.doc;
  const gap = 4;
  const colWidth = (CLINICAL_CONTENT_WIDTH_MM - gap) / 2;
  let row: TemplateElement[] = [];

  const drawRow = (items: TemplateElement[]) => {
    if (items.length === 0) return;
    const fullWidth = items.length === 1;
    const itemWidth = fullWidth ? CLINICAL_CONTENT_WIDTH_MM : colWidth;
    const heights = items.map((item) => measureClinicalField(doc, item, answers?.[item.id], itemWidth).height);
    const rowHeight = Math.max(...heights);
    ensureClinicalSpace(state, rowHeight);
    items.forEach((item, index) => {
      const x = MARGIN_MM + (fullWidth ? 0 : index * (colWidth + gap));
      drawClinicalFieldBox(doc, item, answers?.[item.id], x, state.y, itemWidth, rowHeight);
    });
    state.y += rowHeight + 3;
  };

  const flush = () => {
    drawRow(row);
    row = [];
  };

  elements.forEach((element) => {
    const raw = answers?.[element.id] ?? "";
    const forceFullWidth =
      element.type === "textarea" ||
      element.id === "domicilio" ||
      element.width > 620 ||
      element.label.length > 34 ||
      raw.length > 70;

    if (forceFullWidth) {
      flush();
      drawRow([element]);
      return;
    }

    row.push(element);
    if (row.length === 2) flush();
  });

  flush();
}

function drawClinicalSectionHeader(state: ClinicalRenderState, title: string) {
  if (state.y > CLINICAL_CONTENT_TOP_MM + 1) state.y += 2;
  ensureClinicalSpace(state, 24);

  const doc = state.doc;
  setFillRgb(doc, CLINICAL_LIGHT);
  doc.rect(MARGIN_MM, state.y, CLINICAL_CONTENT_WIDTH_MM, 7.2, "F");
  setFillRgb(doc, CLINICAL_PRIMARY);
  doc.rect(MARGIN_MM, state.y, 1.4, 7.2, "F");
  setTextRgb(doc, [26, 31, 36]);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(9.5);
  doc.text(title, MARGIN_MM + 4, state.y + 4.8);
  state.y += 10.5;
}

function nonEmptyTableRows(value: string | undefined, columnCount: number) {
  return parseTableRows(value, columnCount).filter((row) => row.some((cell) => String(cell ?? "").trim()));
}

function drawClinicalTable(state: ClinicalRenderState, element: TemplateElement, answers?: Record<string, string>) {
  const doc = state.doc;
  const columns = element.columns ?? [];
  if (columns.length === 0) {
    drawClinicalFieldGrid(state, [element], answers);
    return;
  }

  const rows = nonEmptyTableRows(answers?.[element.id], columns.length);
  const x = MARGIN_MM;
  const widthMm = CLINICAL_CONTENT_WIDTH_MM;
  const colWidth = widthMm / columns.length;
  const headerHeight = 7;

  ensureClinicalSpace(state, 16);
  setTextRgb(doc, [28, 33, 38]);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(8.8);
  doc.text(element.label, x, state.y);
  state.y += 3.5;

  const drawHeader = () => {
    ensureClinicalSpace(state, headerHeight + 6);
    setFillRgb(doc, [238, 242, 245]);
    setDrawRgb(doc, CLINICAL_BORDER);
    doc.setLineWidth(0.25);
    doc.rect(x, state.y, widthMm, headerHeight, "FD");
    setTextRgb(doc, [42, 48, 55]);
    doc.setFont("helvetica", "bold");
    doc.setFontSize(7.4);
    columns.forEach((column, index) => {
      const cellX = x + index * colWidth;
      if (index > 0) doc.line(cellX, state.y, cellX, state.y + headerHeight);
      const lines = doc.splitTextToSize(column, colWidth - 3);
      doc.text(lines.slice(0, 2), cellX + 1.5, state.y + 4.2);
    });
    state.y += headerHeight;
  };

  drawHeader();

  if (rows.length === 0) {
    const emptyHeight = 8;
    ensureClinicalSpace(state, emptyHeight);
    setDrawRgb(doc, CLINICAL_BORDER);
    doc.rect(x, state.y, widthMm, emptyHeight);
    setTextRgb(doc, [130, 140, 148]);
    doc.setFont("helvetica", "italic");
    doc.setFontSize(8);
    doc.text("Sin registros", x + 2, state.y + 5);
    state.y += emptyHeight + 4;
    return;
  }

  rows.forEach((row) => {
    doc.setFont("helvetica", "normal");
    doc.setFontSize(7.6);
    const cellLines = columns.map((_, index) => doc.splitTextToSize(String(row[index] ?? ""), colWidth - 3));
    const rowHeight = Math.max(7, Math.max(...cellLines.map((lines) => lines.length)) * 3.6 + 3);
    if (state.y + rowHeight > CLINICAL_CONTENT_BOTTOM_MM) {
      doc.addPage();
      state.y = CLINICAL_CONTENT_TOP_MM;
      drawHeader();
    }

    setDrawRgb(doc, CLINICAL_BORDER);
    doc.rect(x, state.y, widthMm, rowHeight);
    setTextRgb(doc, [35, 40, 45]);
    columns.forEach((_, index) => {
      const cellX = x + index * colWidth;
      if (index > 0) doc.line(cellX, state.y, cellX, state.y + rowHeight);
      doc.text(cellLines[index], cellX + 1.5, state.y + 4.5);
    });
    state.y += rowHeight;
  });

  state.y += 4;
}

function drawClinicalFiles(state: ClinicalRenderState, element: TemplateElement, answers?: Record<string, string>) {
  const attachments = parseAttachments(answers?.[element.id]);
  const value = attachments.length > 0 ? attachments.map((attachment) => attachment.originalFilename).join("\n") : "Sin archivos adjuntos";
  const proxy: TemplateElement = { ...element, type: "textarea" };
  const height = measureClinicalField(state.doc, proxy, value, CLINICAL_CONTENT_WIDTH_MM).height;
  ensureClinicalSpace(state, height);
  drawClinicalFieldBox(state.doc, proxy, value, MARGIN_MM, state.y, CLINICAL_CONTENT_WIDTH_MM, height);
  state.y += height + 3;
}

function drawClinicalOdontogram(state: ClinicalRenderState, element: TemplateElement, answers?: Record<string, string>) {
  const doc = state.doc;
  const x = MARGIN_MM;
  const widthMm = CLINICAL_CONTENT_WIDTH_MM;
  const data = parseOdontogram(answers?.[element.id]);
  const findings = buildOdontogramFindings(answers?.[element.id]);
  const toothSize = Math.min(10.5, (widthMm - 10) / 16);
  const archWidth = toothSize * 16;
  const archX = x + (widthMm - archWidth) / 2;
  const expectedHeight = 72 + odontogramFindingsHeight(doc, findings, widthMm);

  ensureClinicalSpace(state, expectedHeight);
  setTextRgb(doc, [28, 33, 38]);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(9);
  doc.text(element.label, x, state.y);
  state.y += 5;

  const drawArch = (label: string, arch: number[]) => {
    setTextRgb(doc, CLINICAL_MUTED);
    doc.setFont("helvetica", "bold");
    doc.setFontSize(6.7);
    doc.text(label, archX, state.y + 2.2);
    state.y += 3.2;

    arch.forEach((tooth, index) => {
      const tx = archX + index * toothSize;
      const record = data.teeth[String(tooth)] ?? emptyTooth();
      const half = toothSize / 2;
      const cx = tx + half;
      const cy = state.y + half;
      const corners: Record<"tl" | "tr" | "bl" | "br", [number, number]> = {
        tl: [tx, state.y],
        tr: [tx + toothSize, state.y],
        bl: [tx, state.y + toothSize],
        br: [tx + toothSize, state.y + toothSize]
      };

      const fillTriangle = (p1: [number, number], p2: [number, number], surface: "V" | "D" | "M" | "L") => {
        const code = record.surfaces[surface] ?? "sano";
        const colorHex = SURFACE_CONDITION_COLORS[code];
        setDrawRgb(doc, [178, 186, 194]);
        doc.setLineWidth(0.22);
        if (colorHex === "transparent") {
          doc.triangle(p1[0], p1[1], p2[0], p2[1], cx, cy, "S");
          return;
        }
        const [fr, fg, fb] = hexToRgb(colorHex);
        doc.setFillColor(fr, fg, fb);
        doc.triangle(p1[0], p1[1], p2[0], p2[1], cx, cy, "FD");
      };

      fillTriangle(corners.tl, corners.tr, "V");
      fillTriangle(corners.tr, corners.br, "D");
      fillTriangle(corners.br, corners.bl, "L");
      fillTriangle(corners.bl, corners.tl, "M");

      const oclusalCode = record.surfaces.O ?? "sano";
      const oSize = toothSize * 0.32;
      setDrawRgb(doc, [178, 186, 194]);
      if (oclusalCode !== "sano") {
        const [orr, og, ob] = hexToRgb(SURFACE_CONDITION_COLORS[oclusalCode]);
        doc.setFillColor(orr, og, ob);
        doc.rect(cx - oSize / 2, cy - oSize / 2, oSize, oSize, "FD");
      } else {
        doc.rect(cx - oSize / 2, cy - oSize / 2, oSize, oSize, "S");
      }

      doc.rect(tx, state.y, toothSize, toothSize, "S");

      if (record.whole === "ausente") {
        doc.setDrawColor(107, 114, 128);
        doc.line(tx + 0.5, state.y + 0.5, tx + toothSize - 0.5, state.y + toothSize - 0.5);
        doc.line(tx + toothSize - 0.5, state.y + 0.5, tx + 0.5, state.y + toothSize - 0.5);
      } else if (record.whole === "extraccion_indicada") {
        doc.setDrawColor(214, 69, 69);
        doc.line(tx + 0.5, state.y + 0.5, tx + toothSize - 0.5, state.y + toothSize - 0.5);
      } else if (record.whole === "corona") {
        doc.setDrawColor(201, 151, 31);
        doc.rect(tx + 0.4, state.y + 0.4, toothSize - 0.8, toothSize - 0.8, "S");
      } else if (record.whole === "fracturado") {
        doc.setDrawColor(224, 138, 44);
        doc.line(tx + toothSize * 0.1, state.y + toothSize * 0.65, tx + toothSize * 0.35, state.y + toothSize * 0.2);
        doc.line(tx + toothSize * 0.35, state.y + toothSize * 0.2, tx + toothSize * 0.65, state.y + toothSize * 0.8);
        doc.line(tx + toothSize * 0.65, state.y + toothSize * 0.8, tx + toothSize * 0.9, state.y + toothSize * 0.35);
      }

      const badge = WHOLE_CONDITION_BADGES[record.whole];
      if (badge) {
        const badgeR = toothSize * 0.18;
        const badgeCx = tx + toothSize - badgeR;
        const badgeCy = state.y + badgeR;
        const [bgR, bgG, bgB] = hexToRgb(badge.color);
        doc.setFillColor(bgR, bgG, bgB);
        doc.circle(badgeCx, badgeCy, badgeR, "F");
        doc.setTextColor(255, 255, 255);
        doc.setFont("helvetica", "bold");
        doc.setFontSize(Math.max(3, badgeR * 1.25));
        doc.text(badge.text, badgeCx, badgeCy + badgeR * 0.35, { align: "center" });
      }

      const marked = hasToothFinding(record);
      setTextRgb(doc, marked ? [184, 44, 44] : [45, 52, 58]);
      doc.setFont("helvetica", marked ? "bold" : "normal");
      doc.setFontSize(5.2);
      doc.text(String(tooth), cx, state.y + toothSize + 2.8, { align: "center" });
    });

    state.y += toothSize + 7;
  };

  drawArch("Arcada superior", UPPER_ARCH);
  drawArch("Arcada inferior", LOWER_ARCH);
  const legendHeight = drawOdontogramLegend(doc, x, state.y, widthMm);
  state.y += legendHeight + 4;
  const findingsHeight = drawOdontogramFindings(doc, x, state.y, widthMm, findings);
  state.y += findingsHeight + 5;
}

function drawClinicalSignatures(state: ClinicalRenderState, signatures: TemplateElement[], answers?: Record<string, string>) {
  if (signatures.length === 0) return;

  drawClinicalSectionHeader(state, "Firmas");
  const doc = state.doc;
  const gap = 10;
  const width = (CLINICAL_CONTENT_WIDTH_MM - gap) / 2;
  const boxHeight = 31;

  for (let index = 0; index < signatures.length; index += 2) {
    const pair = signatures.slice(index, index + 2);
    ensureClinicalSpace(state, boxHeight + 4);
    pair.forEach((element, pairIndex) => {
      const x = MARGIN_MM + pairIndex * (width + gap);
      const image = getSignatureImageValue(answers?.[element.id]);
      setFillRgb(doc, [255, 255, 255]);
      setDrawRgb(doc, [226, 231, 235]);
      doc.rect(x, state.y, width, boxHeight - 6, "FD");
      if (image) {
        try {
          doc.addImage(image, "PNG", x + 4, state.y + 2, width - 8, boxHeight - 14);
        } catch {
          // firma no disponible; se deja la linea para firma en tinta
        }
      }
      doc.setDrawColor(105, 112, 120);
      doc.line(x + 3, state.y + boxHeight - 10, x + width - 3, state.y + boxHeight - 10);
      setTextRgb(doc, [75, 83, 90]);
      doc.setFont("helvetica", "normal");
      doc.setFontSize(8);
      doc.text(element.label, x + width / 2, state.y + boxHeight - 5.5, { align: "center" });
    });
    state.y += boxHeight + 4;
  }
}

function drawClinicalClinicBlock(state: ClinicalRenderState, info: ResolvedClinicHeaderInfo | undefined) {
  if (!info) return;
  const doc = state.doc;
  const lines = [info.legalName || info.name, info.addressLine, info.phone ? `Tel. ${info.phone}` : undefined, info.email].filter(
    (line): line is string => Boolean(line)
  );
  const logoSize = info.logoDataUrl ? 15 : 0;
  const textX = MARGIN_MM + (logoSize ? logoSize + 4 : 0);
  const textWidth = CLINICAL_CONTENT_WIDTH_MM - (textX - MARGIN_MM) - 3;
  const wrapped = lines.flatMap((line, index) => {
    doc.setFont("helvetica", index === 0 ? "bold" : "normal");
    doc.setFontSize(index === 0 ? 9.5 : 7.8);
    return doc.splitTextToSize(line, textWidth);
  });
  const height = Math.max(18, wrapped.length * 3.8 + 7);

  ensureClinicalSpace(state, height + 3);
  setFillRgb(doc, [248, 251, 252]);
  setDrawRgb(doc, CLINICAL_BORDER);
  doc.rect(MARGIN_MM, state.y, CLINICAL_CONTENT_WIDTH_MM, height, "FD");
  if (info.logoDataUrl) {
    try {
      doc.addImage(info.logoDataUrl, "PNG", MARGIN_MM + 3, state.y + 2.5, logoSize, logoSize);
    } catch {
      // logotipo no disponible; se omite sin interrumpir el PDF
    }
  }

  let cursorY = state.y + 6;
  lines.forEach((line, index) => {
    doc.setFont("helvetica", index === 0 ? "bold" : "normal");
    doc.setFontSize(index === 0 ? 9.5 : 7.8);
    setTextRgb(doc, index === 0 ? [26, 31, 36] : CLINICAL_MUTED);
    const textLines = doc.splitTextToSize(line, textWidth);
    doc.text(textLines, textX + 3, cursorY);
    cursorY += textLines.length * 3.8;
  });

  state.y += height + 5;
}

function drawClinicalPageChrome(doc: jsPDF, title: string, clinicInfo?: ResolvedClinicHeaderInfo) {
  const totalPages = doc.getNumberOfPages();
  for (let page = 1; page <= totalPages; page += 1) {
    doc.setPage(page);
    setTextRgb(doc, [24, 28, 33]);
    doc.setFont("helvetica", "bold");
    doc.setFontSize(11.5);
    doc.text(title, MARGIN_MM, 10);
    setTextRgb(doc, [70, 78, 86]);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(8.5);
    doc.text(`Pagina ${page} de ${totalPages}`, PAGE_WIDTH_MM - MARGIN_MM, 10, { align: "right" });
    setDrawRgb(doc, CLINICAL_PRIMARY);
    doc.setLineWidth(0.35);
    doc.line(MARGIN_MM, 13, PAGE_WIDTH_MM - MARGIN_MM, 13);

    setDrawRgb(doc, [226, 231, 235]);
    doc.setLineWidth(0.2);
    doc.line(MARGIN_MM, PAGE_HEIGHT_MM - 13, PAGE_WIDTH_MM - MARGIN_MM, PAGE_HEIGHT_MM - 13);
    if (clinicInfo?.name || clinicInfo?.email) {
      setTextRgb(doc, [95, 105, 115]);
      doc.setFont("helvetica", "normal");
      doc.setFontSize(7);
      doc.text([clinicInfo.name, clinicInfo.email].filter(Boolean).join(" - "), MARGIN_MM, PAGE_HEIGHT_MM - 8);
    }
  }
}

function renderClinicalElements(state: ClinicalRenderState, elements: TemplateElement[], answers?: Record<string, string>) {
  const sorted = [...elements].sort((a, b) => a.y - b.y || a.x - b.x);
  let simpleBuffer: TemplateElement[] = [];
  let signatureBuffer: TemplateElement[] = [];

  const flushSimpleFields = () => {
    if (simpleBuffer.length === 0) return;
    drawClinicalFieldGrid(state, simpleBuffer, answers);
    simpleBuffer = [];
  };

  const flushSignatures = () => {
    if (signatureBuffer.length === 0) return;
    drawClinicalSignatures(state, signatureBuffer, answers);
    signatureBuffer = [];
  };

  sorted.forEach((element) => {
    if (element.type === "clinic_header") return;

    if (element.type === "signature_patient" || element.type === "signature_doctor") {
      flushSimpleFields();
      signatureBuffer.push(element);
      if (signatureBuffer.length === 2) flushSignatures();
      return;
    }

    if (isClinicalSimpleField(element)) {
      flushSignatures();
      simpleBuffer.push(element);
      return;
    }

    flushSimpleFields();
    flushSignatures();
    if (element.type === "table") drawClinicalTable(state, element, answers);
    else if (element.type === "file") drawClinicalFiles(state, element, answers);
    else if (element.type === "odontogram") drawClinicalOdontogram(state, element, answers);
  });

  flushSimpleFields();
  flushSignatures();
}

function pageHasElement(page: TemplatePage, elementId: string) {
  return page.elements.some((element) => element.id === elementId);
}

function isOrderedOdontologyTemplate(pages: TemplatePage[]) {
  return pages.some((page) => pageHasElement(page, ODONTOLOGY_FIELD_IDS.treatmentConsentText)) &&
    pages.some((page) => pageHasElement(page, ODONTOLOGY_FIELD_IDS.contractText));
}

function clinicalTemplatePageTitle(page: TemplatePage, index: number) {
  if (pageHasElement(page, ODONTOLOGY_FIELD_IDS.treatmentConsentText)) {
    return "Consentimiento informado para tratamientos odontologicos, intervenciones quirurgicas y procedimientos especiales";
  }
  if (pageHasElement(page, APP_INFO_CONSENT_FIELD_IDS.text)) {
    return "Consentimiento informado de uso de Clinerya";
  }
  if (pageHasElement(page, ODONTOLOGY_FIELD_IDS.contractText)) {
    return "Contrato de adhesion para la prestacion de servicios odontologicos";
  }
  if (pageHasElement(page, ODONTOLOGY_FIELD_IDS.odontogram)) {
    return "Declaracion, odontograma e indice CPOD";
  }
  if (pageHasElement(page, ODONTOLOGY_FIELD_IDS.habits)) {
    return "Habitos y antecedentes personales";
  }
  if (pageHasElement(page, ODONTOLOGY_FIELD_IDS.cardiovascular) || pageHasElement(page, ODONTOLOGY_FIELD_IDS.immunologic)) {
    return "Aparatos y sistemas";
  }
  return index === 0 ? "Historia clinica odontologica" : `Hoja ${index + 1}`;
}

function drawClinicalPagesInTemplateOrder(
  doc: jsPDF,
  title: string,
  pages: TemplatePage[],
  answers?: Record<string, string>,
  clinicInfo?: ResolvedClinicHeaderInfo
) {
  const state: ClinicalRenderState = { doc, y: CLINICAL_CONTENT_TOP_MM };

  pages.forEach((page, index) => {
    if (index > 0) doc.addPage();
    state.y = CLINICAL_CONTENT_TOP_MM;
    if (index === 0) drawClinicalClinicBlock(state, clinicInfo);
    drawClinicalSectionHeader(state, clinicalTemplatePageTitle(page, index));
    renderClinicalElements(state, page.elements, answers);
  });

  drawClinicalPageChrome(doc, title, clinicInfo);
}

function drawClinicalPages(
  doc: jsPDF,
  title: string,
  pages: TemplatePage[],
  answers?: Record<string, string>,
  clinicInfo?: ResolvedClinicHeaderInfo
) {
  if (isOrderedOdontologyTemplate(pages)) {
    drawClinicalPagesInTemplateOrder(doc, title, pages, answers, clinicInfo);
    return;
  }

  const state: ClinicalRenderState = { doc, y: CLINICAL_CONTENT_TOP_MM };
  const allElements = pages.flatMap((page) => page.elements);
  const signatures = allElements.filter((element) => (element.type === "signature_patient" || element.type === "signature_doctor") && !element.sectionId);
  const consentElements = allElements.filter((element) => element.sectionId === CONSENT_SECTION_ID);
  const rendered = new Set<string>();

  drawClinicalClinicBlock(state, clinicInfo);

  [...NOM_SECTIONS]
    .filter((section) => section.id !== CONSENT_SECTION_ID)
    .sort((a, b) => a.order - b.order)
    .forEach((section) => {
      const sectionElements = allElements.filter((element) => element.sectionId === section.id);
      if (sectionElements.length === 0) return;
      sectionElements.forEach((element) => rendered.add(element.id));
      drawClinicalSectionHeader(state, section.title);
      renderClinicalElements(state, sectionElements, answers);
    });

  const remaining = allElements.filter(
    (element) =>
      !rendered.has(element.id) &&
      element.sectionId !== CONSENT_SECTION_ID &&
      element.type !== "clinic_header" &&
      element.type !== "signature_patient" &&
      element.type !== "signature_doctor"
  );
  if (remaining.length > 0) {
    drawClinicalSectionHeader(state, "Datos adicionales");
    renderClinicalElements(state, remaining, answers);
  }

  drawClinicalSignatures(state, signatures, answers);

  if (consentElements.length > 0) {
    const consentSection = NOM_SECTIONS.find((section) => section.id === CONSENT_SECTION_ID);
    doc.addPage();
    state.y = CLINICAL_CONTENT_TOP_MM;
    drawClinicalSectionHeader(state, consentSection?.title ?? "Consentimiento expreso y por escrito");
    renderClinicalElements(state, consentElements, answers);
  }

  drawClinicalPageChrome(doc, title, clinicInfo);
}

function drawPage(doc: jsPDF, title: string, pageNumber: number, totalPages: number, page: TemplatePage, canvasWidthPx: number, answers?: Record<string, string>, clinicInfo?: ResolvedClinicHeaderInfo) {
  doc.setTextColor(26, 26, 26);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(11);
  doc.text(title, MARGIN_MM, 10);
  doc.setFont("helvetica", "normal");
  doc.setFontSize(9);
  doc.text(`Página ${pageNumber} de ${totalPages}`, PAGE_WIDTH_MM - MARGIN_MM, 10, { align: "right" });
  doc.setDrawColor(200);
  doc.line(MARGIN_MM, 13, PAGE_WIDTH_MM - MARGIN_MM, 13);

  [...page.elements]
    .sort((a, b) => a.y - b.y || a.x - b.x)
    .forEach((element) => {
      const value = answers ? answers[element.id] : undefined;
      if (element.type === "table") {
        drawTableElement(doc, element, canvasWidthPx, value);
      } else if (element.type === "file") {
        drawFileElement(doc, element, canvasWidthPx, value);
      } else if (element.type === "odontogram") {
        drawOdontogramElement(doc, element, canvasWidthPx, value);
      } else if (element.type === "clinic_header") {
        drawClinicHeaderElement(doc, element, canvasWidthPx, clinicInfo);
      } else if (element.type === "signature_patient" || element.type === "signature_doctor") {
        drawSignatureElement(doc, element, canvasWidthPx, value);
      } else {
        drawElement(doc, element, canvasWidthPx, value);
      }
    });
}

export interface PdfExportRange {
  from?: number;
  to?: number;
}

export interface PdfExportOptions {
  title: string;
  pages: TemplatePage[];
  canvasWidthPx: number;
  answers?: Record<string, string>;
  range?: PdfExportRange;
  clinicInfo?: ClinicHeaderInfo;
  clinicalLayout?: boolean;
}

export async function createPagesPdfDocument(options: PdfExportOptions) {
  const { title, pages, canvasWidthPx, answers, range, clinicInfo, clinicalLayout } = options;
  const startIdx = Math.max(0, (range?.from ?? 1) - 1);
  const endIdx = Math.min(pages.length - 1, (range?.to ?? pages.length) - 1);
  const selected = pages.slice(startIdx, endIdx + 1);
  if (selected.length === 0) return null;

  const resolvedClinicInfo: ResolvedClinicHeaderInfo | undefined = clinicInfo
    ? { ...clinicInfo, logoDataUrl: clinicInfo.logoUrl ? (await loadImageAsDataUrl(clinicInfo.logoUrl)) ?? undefined : undefined }
    : undefined;

  const doc = new jsPDF({ unit: "mm", format: "letter" });
  if (clinicalLayout) {
    drawClinicalPages(doc, title, selected, answers, resolvedClinicInfo);
  } else {
    selected.forEach((page, index) => {
      if (index > 0) doc.addPage();
      drawPage(doc, title, startIdx + index + 1, pages.length, page, canvasWidthPx, answers, resolvedClinicInfo);
    });
  }

  return doc;
}

export async function exportPagesToPdf(options: PdfExportOptions) {
  const doc = await createPagesPdfDocument(options);
  if (!doc) return;

  const safeName = options.title.trim().replace(/[^a-z0-9áéíóúñü_\- ]/gi, "").replace(/\s+/g, "_") || "documento";
  doc.save(`${safeName}.pdf`);
}
