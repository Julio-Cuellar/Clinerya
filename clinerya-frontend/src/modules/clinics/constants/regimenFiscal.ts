/**
 * Catálogo oficial c_RegimenFiscal del SAT (Anexo 20, CFDI 4.0).
 * Fuente: https://www.sat.gob.mx/consultas/formato-de-factura-electronica
 * Catálogo estático: el SAT lo publica como documento (XLS/PDF), no como API.
 */
export const regimenesFiscales: Array<{ code: string; label: string }> = [
  { code: "601", label: "601 - General de Ley Personas Morales" },
  { code: "603", label: "603 - Personas Morales con Fines no Lucrativos" },
  { code: "605", label: "605 - Sueldos y Salarios e Ingresos Asimilados a Salarios" },
  { code: "606", label: "606 - Arrendamiento" },
  { code: "607", label: "607 - Régimen de Enajenación o Adquisición de Bienes" },
  { code: "608", label: "608 - Demás ingresos" },
  { code: "610", label: "610 - Residentes en el Extranjero sin Establecimiento Permanente en México" },
  { code: "611", label: "611 - Ingresos por Dividendos (socios y accionistas)" },
  { code: "612", label: "612 - Personas Físicas con Actividades Empresariales y Profesionales" },
  { code: "614", label: "614 - Ingresos por intereses" },
  { code: "615", label: "615 - Régimen de los ingresos por obtención de premios" },
  { code: "616", label: "616 - Sin obligaciones fiscales" },
  { code: "620", label: "620 - Sociedades Cooperativas de Producción que optan por diferir sus ingresos" },
  { code: "621", label: "621 - Incorporación Fiscal" },
  { code: "622", label: "622 - Actividades Agrícolas, Ganaderas, Silvícolas y Pesqueras" },
  { code: "623", label: "623 - Opcional para Grupos de Sociedades" },
  { code: "624", label: "624 - Coordinados" },
  { code: "625", label: "625 - Régimen de las Actividades Empresariales con ingresos a través de Plataformas Tecnológicas" },
  { code: "626", label: "626 - Régimen Simplificado de Confianza (RESICO)" }
];
