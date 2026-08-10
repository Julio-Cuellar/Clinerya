import { genPageId, type TemplateElement, type TemplateFieldType, type TemplatePage } from "@modules/records/types";

export interface NomFieldSuggestion {
  id: string;
  label: string;
  type: TemplateFieldType;
  options?: string[];
  columns?: string[];
}

export interface NomSection {
  id: string;
  title: string;
  order: number;
  minElements: number;
  bandHeight: number;
  suggestedFields: NomFieldSuggestion[];
}

export const CONSENT_SECTION_ID = "consentimiento_expreso";

export const CONSENT_FIELD_IDS = {
  text: "consentimientoExpresoTexto",
  signerName: "consentimientoFirmante",
  date: "consentimientoFecha",
  patientSignature: "firmaConsentimientoPaciente",
  doctorSignature: "firmaConsentimientoMedico"
} as const;

export const DEFAULT_CONSENT_TEXT =
  "Por medio del presente manifiesto que he recibido informacion clara, suficiente y comprensible sobre la integracion de mi historia clinica, la exploracion, el diagnostico, el plan de tratamiento y el manejo de mis datos personales sensibles de salud. Otorgo mi consentimiento expreso y por escrito para que la clinica y su personal autorizado registren, conserven y utilicen esta informacion exclusivamente para fines de atencion medica u odontologica, seguimiento clinico y cumplimiento de las disposiciones sanitarias aplicables.";

export const NOM_SECTIONS: NomSection[] = [
  {
    id: "ficha_identificacion",
    title: "Ficha de identificación",
    order: 1,
    minElements: 10,
    bandHeight: 380,
    suggestedFields: [
      { id: "nombre", label: "Nombre completo", type: "text" },
      { id: "sexo", label: "Sexo", type: "select", options: ["Masculino", "Femenino"] },
      { id: "edad", label: "Edad", type: "number" },
      { id: "domicilio", label: "Domicilio", type: "text" },
      { id: "fechaNacimiento", label: "Fecha de nacimiento", type: "date" },
      { id: "lugarNacimiento", label: "Lugar de nacimiento", type: "text" },
      { id: "telefono", label: "Teléfono", type: "text" },
      { id: "escolaridad", label: "Escolaridad", type: "text" },
      { id: "estadoCivil", label: "Estado civil", type: "text" },
      { id: "ocupacion", label: "Ocupación", type: "text" },
      { id: "religion", label: "Religión", type: "text" },
      { id: "tipoSangreRh", label: "Tipo de sangre y RH", type: "text" },
      { id: "alergias", label: "Alergias", type: "table", columns: ["Alérgeno", "Tipo de reacción"] }
    ]
  },
  {
    id: "antecedentes_heredofamiliares",
    title: "Antecedentes heredofamiliares",
    order: 2,
    minElements: 1,
    bandHeight: 190,
    suggestedFields: [
      {
        id: "antecedentesHeredofamiliares",
        label: "Antecedentes heredofamiliares",
        type: "table",
        columns: ["Parentesco", "Padecimiento", "Vivo/Finado", "Edad de inicio"]
      }
    ]
  },
  {
    id: "antecedentes_patologicos",
    title: "Antecedentes personales patológicos",
    order: 3,
    minElements: 1,
    bandHeight: 190,
    suggestedFields: [
      {
        id: "antecedentesPatologicos",
        label: "Antecedentes personales patológicos",
        type: "table",
        columns: ["Padecimiento/Evento", "Fecha", "Tratamiento/Observaciones"]
      }
    ]
  },
  {
    id: "antecedentes_no_patologicos",
    title: "Antecedentes personales no patológicos",
    order: 4,
    minElements: 3,
    bandHeight: 200,
    suggestedFields: [
      { id: "tabaquismo", label: "Tabaquismo", type: "text" },
      { id: "alcoholismo", label: "Alcoholismo", type: "text" },
      { id: "otrasAdicciones", label: "Otras adicciones", type: "text" },
      { id: "actividadFisica", label: "Actividad física", type: "text" },
      { id: "alimentacion", label: "Alimentación", type: "text" },
      { id: "higiene", label: "Higiene", type: "text" }
    ]
  },
  {
    id: "esquema_vacunacion",
    title: "Esquema de vacunación",
    order: 5,
    minElements: 1,
    bandHeight: 170,
    suggestedFields: [
      {
        id: "esquemaVacunacion",
        label: "Esquema de vacunación",
        type: "table",
        columns: ["Vacuna", "Fecha de aplicación", "Dosis/Refuerzo"]
      }
    ]
  },
  {
    id: "padecimiento_actual",
    title: "Padecimiento actual",
    order: 6,
    minElements: 1,
    bandHeight: 180,
    suggestedFields: [{ id: "motivoConsultaEvolucion", label: "Motivo de consulta y evolución", type: "textarea" }]
  },
  {
    id: "interrogatorio_aparatos_sistemas",
    title: "Interrogatorio por aparatos y sistemas",
    order: 7,
    minElements: 3,
    bandHeight: 220,
    suggestedFields: [
      { id: "cardiovascular", label: "Cardiovascular", type: "textarea" },
      { id: "respiratorio", label: "Respiratorio", type: "textarea" },
      { id: "digestivo", label: "Digestivo", type: "textarea" },
      { id: "genitourinario", label: "Genitourinario", type: "textarea" },
      { id: "neurologico", label: "Neurológico", type: "textarea" },
      { id: "musculoesqueletico", label: "Musculoesquelético", type: "textarea" }
    ]
  },
  {
    id: "exploracion_fisica",
    title: "Exploración física y signos vitales",
    order: 8,
    minElements: 5,
    bandHeight: 420,
    suggestedFields: [
      { id: "tensionArterial", label: "Tensión arterial", type: "text" },
      { id: "frecuenciaCardiaca", label: "Frecuencia cardiaca", type: "number" },
      { id: "frecuenciaRespiratoria", label: "Frecuencia respiratoria", type: "number" },
      { id: "temperatura", label: "Temperatura", type: "number" },
      { id: "peso", label: "Peso", type: "number" },
      { id: "talla", label: "Talla", type: "number" },
      { id: "cabezaCuello", label: "Cabeza y cuello", type: "textarea" },
      { id: "torax", label: "Tórax", type: "textarea" },
      { id: "abdomen", label: "Abdomen", type: "textarea" },
      { id: "extremidades", label: "Extremidades", type: "textarea" },
      {
        id: "historialSignosVitales",
        label: "Historial de signos vitales",
        type: "table",
        columns: ["Fecha", "TA", "FC", "FR", "Temp", "Peso", "Talla", "SpO2"]
      }
    ]
  },
  {
    id: "resultados_estudios",
    title: "Resultados de estudios",
    order: 9,
    minElements: 1,
    bandHeight: 320,
    suggestedFields: [
      {
        id: "resultadosLaboratorio",
        label: "Resultados de laboratorio",
        type: "table",
        columns: ["Estudio", "Fecha", "Resultado"]
      },
      {
        id: "estudiosGabinete",
        label: "Estudios de gabinete (imágenes, PDF)",
        type: "file"
      }
    ]
  },
  {
    id: "diagnosticos",
    title: "Diagnósticos",
    order: 10,
    minElements: 1,
    bandHeight: 160,
    suggestedFields: [{ id: "diagnosticoPresuntivoDefinitivo", label: "Diagnóstico presuntivo o definitivo", type: "textarea" }]
  },
  {
    id: "pronostico",
    title: "Pronóstico",
    order: 11,
    minElements: 1,
    bandHeight: 160,
    suggestedFields: [{ id: "pronostico", label: "Pronóstico", type: "textarea" }]
  },
  {
    id: "indicacion_terapeutica",
    title: "Indicación terapéutica",
    order: 12,
    minElements: 1,
    bandHeight: 340,
    suggestedFields: [
      { id: "planTratamiento", label: "Plan de tratamiento", type: "textarea" },
      {
        id: "medicamentosActuales",
        label: "Medicamentos actuales",
        type: "table",
        columns: ["Medicamento", "Dosis", "Frecuencia", "Vía"]
      }
    ]
  },
  {
    id: CONSENT_SECTION_ID,
    title: "Consentimiento expreso y por escrito",
    order: 13,
    minElements: 1,
    bandHeight: 700,
    suggestedFields: [
      { id: CONSENT_FIELD_IDS.text, label: "Consentimiento expreso y por escrito", type: "textarea" },
      { id: CONSENT_FIELD_IDS.signerName, label: "Nombre del paciente o representante legal", type: "text" },
      { id: CONSENT_FIELD_IDS.date, label: "Fecha", type: "date" },
      { id: CONSENT_FIELD_IDS.patientSignature, label: "Firma del paciente o representante legal", type: "signature_patient" },
      { id: CONSENT_FIELD_IDS.doctorSignature, label: "Firma del medico tratante", type: "signature_doctor" }
    ]
  }
];

// Hoja tamaño carta: mismas proporciones usadas al exportar a PDF (216 x 279 mm, márgenes de 14mm).
export const CANVAS_WIDTH = 900;
const PAGE_WIDTH_MM = 216;
const PAGE_HEIGHT_MM = 279;
const PAGE_MARGIN_MM = 14;
const CONTENT_TOP_MM = 22;
const USABLE_WIDTH_MM = PAGE_WIDTH_MM - PAGE_MARGIN_MM * 2;
const USABLE_HEIGHT_MM = PAGE_HEIGHT_MM - PAGE_MARGIN_MM - CONTENT_TOP_MM;
export const CANVAS_PAGE_HEIGHT = Math.round((USABLE_HEIGHT_MM / USABLE_WIDTH_MM) * CANVAS_WIDTH);

export const ELEMENT_MARGIN = 16;
const DEFAULT_ELEMENT_WIDTH = 400;

function defaultHeightForType(type: TemplateFieldType) {
  if (type === "odontogram") return 320;
  if (type === "table") return 150;
  if (type === "file") return 110;
  if (type === "textarea") return 90;
  if (type === "clinic_header") return 90;
  if (type === "signature_patient" || type === "signature_doctor") return 120;
  return 56;
}

export function bandBoundaries(): Array<{ sectionId: string; title: string; top: number; bottom: number; minElements: number }> {
  const sections = [...NOM_SECTIONS].sort((a, b) => a.order - b.order);
  let cursor = 0;
  return sections.map((section) => {
    const top = cursor;
    const bottom = cursor + section.bandHeight;
    cursor = bottom;
    return { sectionId: section.id, title: section.title, top, bottom, minElements: section.minElements };
  });
}

export function sectionIdForY(y: number): string | undefined {
  const bands = bandBoundaries();
  const found = bands.find((band) => y >= band.top && y < band.bottom);
  return found?.sectionId ?? bands[bands.length - 1]?.sectionId;
}

export function totalCanvasHeight(): number {
  const bands = bandBoundaries();
  return bands.length > 0 ? bands[bands.length - 1].bottom : 0;
}

export function buildConsentPage(): TemplatePage {
  const fullWidth = CANVAS_WIDTH - ELEMENT_MARGIN * 2;
  const halfWidth = Math.floor((fullWidth - 20) / 2);
  const signatureSize = defaultSizeForType("signature_patient");
  const signatureY = 550;

  const elements: TemplateElement[] = [
    {
      id: CONSENT_FIELD_IDS.text,
      sectionId: CONSENT_SECTION_ID,
      label: "Consentimiento expreso y por escrito",
      type: "textarea",
      x: ELEMENT_MARGIN,
      y: ELEMENT_MARGIN,
      width: fullWidth,
      height: 390,
      align: "left",
      fontSize: 11
    },
    {
      id: CONSENT_FIELD_IDS.signerName,
      sectionId: CONSENT_SECTION_ID,
      label: "Nombre del paciente o representante legal",
      type: "text",
      x: ELEMENT_MARGIN,
      y: 430,
      width: halfWidth,
      height: 56,
      align: "left"
    },
    {
      id: CONSENT_FIELD_IDS.date,
      sectionId: CONSENT_SECTION_ID,
      label: "Fecha",
      type: "date",
      x: ELEMENT_MARGIN + halfWidth + 20,
      y: 430,
      width: halfWidth,
      height: 56,
      align: "left"
    },
    {
      id: CONSENT_FIELD_IDS.patientSignature,
      sectionId: CONSENT_SECTION_ID,
      label: "Firma del paciente o representante legal",
      type: "signature_patient",
      x: ELEMENT_MARGIN,
      y: signatureY,
      width: signatureSize.width + 90,
      height: signatureSize.height
    },
    {
      id: CONSENT_FIELD_IDS.doctorSignature,
      sectionId: CONSENT_SECTION_ID,
      label: "Firma del medico tratante",
      type: "signature_doctor",
      x: ELEMENT_MARGIN + signatureSize.width + 130,
      y: signatureY,
      width: signatureSize.width + 90,
      height: signatureSize.height
    }
  ];

  return { id: genPageId(), elements, canvasHeight: CANVAS_PAGE_HEIGHT };
}

export function hasConsentPage(pages: TemplatePage[]): boolean {
  return pages.some((page) =>
    page.elements.some((element) => element.sectionId === CONSENT_SECTION_ID || element.id === CONSENT_FIELD_IDS.text)
  );
}

export function ensureConsentPage(pages: TemplatePage[]): TemplatePage[] {
  return hasConsentPage(pages) ? pages : [...pages, buildConsentPage()];
}

/**
 * Distribuye los campos sugeridos de la NOM-004 en una cuadrícula de 2 columnas,
 * paginada en hojas tamaño carta (CANVAS_PAGE_HEIGHT), lista para usarse sin
 * necesidad de reacomodar nada: los campos cortos van en pares lado a lado y los
 * de texto largo (párrafo) ocupan el ancho completo de la hoja.
 */
export function buildNomStarterPages(options?: { firstPageTopOffset?: number; firstPageBottomReserve?: number; includeConsentPage?: boolean }): TemplatePage[] {
  const firstPageTopOffset = options?.firstPageTopOffset ?? 0;
  const firstPageBottomReserve = options?.firstPageBottomReserve ?? 0;
  const includeConsentPage = options?.includeConsentPage ?? true;

  const GUTTER = 20;
  const ROW_GAP = 14;
  const SECTION_GAP = 26;
  const HALF_WIDTH = Math.floor((CANVAS_WIDTH - ELEMENT_MARGIN * 2 - GUTTER) / 2);
  const FULL_WIDTH = CANVAS_WIDTH - ELEMENT_MARGIN * 2;
  const PAGE_BOTTOM = CANVAS_PAGE_HEIGHT - ELEMENT_MARGIN;

  const pages: TemplatePage[] = [];
  let elements: TemplateElement[] = [];
  let isFirstPage = true;
  let cursorY = ELEMENT_MARGIN + firstPageTopOffset;
  let col: 0 | 1 = 0;
  let rowHeight = 0;

  const pageBottom = () => (isFirstPage ? PAGE_BOTTOM - firstPageBottomReserve : PAGE_BOTTOM);

  const finishRow = () => {
    if (col === 1) {
      cursorY += rowHeight + ROW_GAP;
      col = 0;
      rowHeight = 0;
    }
  };

  const flushPage = () => {
    if (elements.length > 0) {
      pages.push({ id: genPageId(), elements, canvasHeight: CANVAS_PAGE_HEIGHT });
    }
    elements = [];
    isFirstPage = false;
    cursorY = ELEMENT_MARGIN;
    col = 0;
    rowHeight = 0;
  };

  const ensureSpace = (height: number) => {
    if (cursorY + height > pageBottom()) {
      flushPage();
    }
  };

  [...NOM_SECTIONS]
    .filter((section) => section.id !== CONSENT_SECTION_ID)
    .sort((a, b) => a.order - b.order)
    .forEach((section, sectionIndex) => {
      finishRow();
      if (sectionIndex > 0) cursorY += SECTION_GAP;

      section.suggestedFields.forEach((field) => {
        const height = defaultHeightForType(field.type);
        const isFullWidth = field.type === "textarea" || field.type === "table" || field.type === "file";

        if (isFullWidth) {
          finishRow();
          ensureSpace(height);
          elements.push({
            id: field.id,
            sectionId: section.id,
            label: field.label,
            type: field.type,
            options: field.options,
            columns: field.columns,
            x: ELEMENT_MARGIN,
            y: cursorY,
            width: FULL_WIDTH,
            height
          });
          cursorY += height + ROW_GAP;
          return;
        }

        if (col === 0) {
          ensureSpace(height);
          rowHeight = height;
          elements.push({
            id: field.id,
            sectionId: section.id,
            label: field.label,
            type: field.type,
            options: field.options,
            columns: field.columns,
            x: ELEMENT_MARGIN,
            y: cursorY,
            width: HALF_WIDTH,
            height
          });
          col = 1;
        } else {
          elements.push({
            id: field.id,
            sectionId: section.id,
            label: field.label,
            type: field.type,
            options: field.options,
            columns: field.columns,
            x: ELEMENT_MARGIN + HALF_WIDTH + GUTTER,
            y: cursorY,
            width: HALF_WIDTH,
            height
          });
          rowHeight = Math.max(rowHeight, height);
          cursorY += rowHeight + ROW_GAP;
          col = 0;
          rowHeight = 0;
        }
      });
    });

  flushPage();
  return includeConsentPage ? ensureConsentPage(pages) : pages;
}

export function checkCompliance(elements: TemplateElement[]): { compliant: boolean; missingSections: string[] } {
  const missingSections: string[] = [];

  NOM_SECTIONS.forEach((section) => {
    const count = elements.filter((element) => element.sectionId === section.id).length;
    if (count < section.minElements) {
      missingSections.push(section.id);
    }
  });

  return { compliant: missingSections.length === 0, missingSections };
}

export const FREE_ELEMENT_TYPES: Array<{ type: TemplateFieldType; label: string; columns?: string[] }> = [
  { type: "text", label: "Texto corto" },
  { type: "textarea", label: "Párrafo" },
  { type: "number", label: "Número" },
  { type: "date", label: "Fecha" },
  { type: "select", label: "Lista desplegable" },
  { type: "table", label: "Tabla", columns: ["Columna 1", "Columna 2"] },
  { type: "file", label: "Adjuntar archivo" },
  { type: "odontogram", label: "Odontograma" },
  { type: "clinic_header", label: "Logotipo y datos de la clínica" },
  { type: "signature_patient", label: "Firma del paciente" },
  { type: "signature_doctor", label: "Firma del médico" }
];

export function defaultSizeForType(type: TemplateFieldType) {
  if (type === "odontogram") return { width: 780, height: defaultHeightForType(type) };
  if (type === "table" || type === "file") return { width: 640, height: defaultHeightForType(type) };
  if (type === "clinic_header") return { width: 320, height: defaultHeightForType(type) };
  if (type === "signature_patient" || type === "signature_doctor") return { width: 260, height: defaultHeightForType(type) };
  return { width: DEFAULT_ELEMENT_WIDTH, height: defaultHeightForType(type) };
}
