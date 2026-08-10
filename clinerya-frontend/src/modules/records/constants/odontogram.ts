export type ToothSurfaceCode = "V" | "D" | "M" | "L" | "O";

export type SurfaceConditionCode =
  | "sano"
  | "caries"
  | "mancha_blanca"
  | "obturado"
  | "sellante"
  | "desgaste"
  | "fractura_cara"
  | "caries_secundaria";

export type WholeToothConditionCode =
  | "sano"
  | "ausente"
  | "extraccion_indicada"
  | "corona"
  | "fracturado"
  | "por_erupcionar"
  | "movilidad"
  | "endodoncia"
  | "protesis_fija"
  | "protesis_removible"
  | "implante"
  | "supernumerario";

export interface ToothRecord {
  surfaces: Partial<Record<ToothSurfaceCode, SurfaceConditionCode>>;
  whole: WholeToothConditionCode;
}

export interface OdontogramData {
  teeth: Record<string, ToothRecord>;
}

export interface CpodIndex {
  decayed: number;
  missing: number;
  filled: number;
  total: number;
}

// Numeración FDI (ISO 3950), la misma que usan los odontogramas de expediente clínico dental en México (NOM-013-SSA2-2015).
export const UPPER_ARCH = [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28];
export const LOWER_ARCH = [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38];
const PERMANENT_TEETH = new Set([...UPPER_ARCH, ...LOWER_ARCH].map(String));

export const SURFACE_LABELS: Record<ToothSurfaceCode, string> = {
  V: "Vestibular",
  D: "Distal",
  M: "Mesial",
  L: "Palatino/Lingual",
  O: "Oclusal/Incisal"
};

export const SURFACE_CONDITION_ORDER: SurfaceConditionCode[] = [
  "sano",
  "caries",
  "mancha_blanca",
  "obturado",
  "caries_secundaria",
  "sellante",
  "desgaste",
  "fractura_cara"
];

export const SURFACE_CONDITION_LABELS: Record<SurfaceConditionCode, string> = {
  sano: "Sano",
  caries: "Caries",
  mancha_blanca: "Mancha blanca (lesión incipiente)",
  obturado: "Obturado/restaurado",
  caries_secundaria: "Caries secundaria (bajo restauración)",
  sellante: "Sellante",
  desgaste: "Desgaste/abrasión",
  fractura_cara: "Fractura de la cara"
};

export const SURFACE_CONDITION_COLORS: Record<SurfaceConditionCode, string> = {
  sano: "transparent",
  caries: "#D64545",
  mancha_blanca: "#E8D96B",
  obturado: "#2F6FED",
  caries_secundaria: "#B02E7A",
  sellante: "#2E9E5B",
  desgaste: "#9B7653",
  fractura_cara: "#E08A2C"
};

export const WHOLE_CONDITION_ORDER: WholeToothConditionCode[] = [
  "sano",
  "ausente",
  "extraccion_indicada",
  "corona",
  "fracturado",
  "por_erupcionar",
  "movilidad",
  "endodoncia",
  "protesis_fija",
  "protesis_removible",
  "implante",
  "supernumerario"
];

export const WHOLE_CONDITION_LABELS: Record<WholeToothConditionCode, string> = {
  sano: "Sano",
  ausente: "Ausente",
  extraccion_indicada: "Extracción indicada",
  corona: "Corona",
  fracturado: "Fracturado",
  por_erupcionar: "Por erupcionar/retenido",
  movilidad: "Movilidad dental",
  endodoncia: "Tratamiento de conducto (endodoncia)",
  protesis_fija: "Prótesis fija/puente",
  protesis_removible: "Prótesis removible",
  implante: "Implante",
  supernumerario: "Diente supernumerario"
};

// Para los estados de pieza completa que no tienen un dibujo dedicado en el SVG, se muestra una
// insignia con esta abreviatura y color en la esquina del diente.
export const WHOLE_CONDITION_BADGES: Partial<Record<WholeToothConditionCode, { text: string; color: string }>> = {
  por_erupcionar: { text: "E", color: "#829A9A" },
  movilidad: { text: "M+", color: "#E08A2C" },
  endodoncia: { text: "TC", color: "#7A3FA0" },
  protesis_fija: { text: "PF", color: "#2F6FED" },
  protesis_removible: { text: "PR", color: "#2F6FED" },
  implante: { text: "IM", color: "#4A6A6A" },
  supernumerario: { text: "SN", color: "#7A3FA0" }
};

export function emptyTooth(): ToothRecord {
  return { surfaces: {}, whole: "sano" };
}

export function parseOdontogram(raw: string | undefined): OdontogramData {
  if (!raw) return { teeth: {} };
  try {
    const parsed = JSON.parse(raw);
    if (parsed && typeof parsed === "object" && typeof parsed.teeth === "object" && parsed.teeth !== null) {
      return parsed as OdontogramData;
    }
  } catch {
    // valor previo no es un odontograma serializado; se descarta y se arranca vacío
  }
  return { teeth: {} };
}

export function serializeOdontogram(data: OdontogramData): string {
  return JSON.stringify(data);
}

export function calculateCpodIndex(raw: string | undefined): CpodIndex {
  const data = parseOdontogram(raw);
  let decayed = 0;
  let missing = 0;
  let filled = 0;

  Object.entries(data.teeth).forEach(([tooth, record]) => {
    if (!PERMANENT_TEETH.has(tooth)) return;

    const surfaceConditions = Object.values(record.surfaces ?? {});
    const hasDecay = surfaceConditions.some((condition) => condition === "caries" || condition === "caries_secundaria");
    const hasFilling = surfaceConditions.some((condition) => condition === "obturado") || record.whole === "corona";

    if (record.whole === "ausente") {
      missing += 1;
    } else if (hasDecay) {
      decayed += 1;
    } else if (hasFilling) {
      filled += 1;
    }
  });

  return { decayed, missing, filled, total: decayed + missing + filled };
}
