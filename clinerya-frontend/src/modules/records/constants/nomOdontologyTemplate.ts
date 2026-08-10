import { genPageId, type TemplateElement, type TemplatePage } from "@modules/records/types";
import { CANVAS_PAGE_HEIGHT, CANVAS_WIDTH, CONSENT_SECTION_ID, ELEMENT_MARGIN, defaultSizeForType } from "./nomHistoryTemplate";

const HEADER_HEIGHT = 90;
const HEADER_GAP = 18;
const HEADER_OFFSET = HEADER_HEIGHT + HEADER_GAP;
const GUTTER = 20;
const ROW_GAP = 14;
const FULL_WIDTH = CANVAS_WIDTH - ELEMENT_MARGIN * 2;
const HALF_WIDTH = Math.floor((FULL_WIDTH - GUTTER) / 2);
const THIRD_WIDTH = Math.floor((FULL_WIDTH - GUTTER * 2) / 3);
const YES_NO_OPTIONS = ["Si", "No"];

const SECTIONS = {
  identification: "ficha_identificacion",
  family: "antecedentes_heredofamiliares",
  pathological: "antecedentes_patologicos",
  nonPathological: "antecedentes_no_patologicos",
  vaccination: "esquema_vacunacion",
  current: "padecimiento_actual",
  systems: "interrogatorio_aparatos_sistemas",
  diagnosis: "diagnosticos",
  prognosis: "pronostico",
  treatment: "indicacion_terapeutica"
} as const;

export const ODONTOLOGY_FIELD_IDS = {
  objectAndConfidentiality: "objetoConfidencialidadDental",
  familyHistory: "antecedentesHeredofamiliaresDental",
  immunizations: "inmunizacionesDental",
  habits: "habitosDentales",
  diet: "alimentacionDental",
  pathologicalHistory: "antecedentesPatologicosDental",
  currentCondition: "padecimientoActualDental",
  cardiovascular: "sistemaCardiovascularDental",
  endocrine: "sistemaEndocrinoDental",
  digestive: "sistemaDigestivoDental",
  respiratory: "sistemaRespiratorioDental",
  immunologic: "sistemaInmunologicoDental",
  musculoskeletal: "sistemaMusculoesqueleticoDental",
  nervous: "sistemaNerviosoDental",
  lymphGenitourinary: "sistemaLinfoGenitourinarioDental",
  additionalHealthInfo: "informacionAdicionalSaludDental",
  truthStatement: "declaracionVeracidadDental",
  dentalDiagnosis: "diagnosticoDental",
  dentalTreatmentPlan: "planTratamientoDental",
  dentalPrognosis: "pronosticoDental",
  odontogram: "odontogramaDental",
  cpod: "indiceCpodDental",
  odontogramNotes: "observacionesOdontogramaDental",
  treatmentConsentText: "consentimientoTratamientoOdontologicoTexto",
  treatmentConsentPatient: "consentimientoTratamientoPaciente",
  treatmentConsentAge: "consentimientoTratamientoEdad",
  treatmentConsentDate: "consentimientoTratamientoFecha",
  treatmentConsentCity: "consentimientoTratamientoCiudad",
  treatmentConsentProcedure: "consentimientoTratamientoProcedimiento",
  treatmentConsentPrognosis: "consentimientoTratamientoPronostico",
  treatmentConsentGeneralRisks: "consentimientoTratamientoRiesgosGenerales",
  treatmentConsentSpecificRisks: "consentimientoTratamientoRiesgosEspecificos",
  treatmentConsentWitness1: "consentimientoTratamientoTestigo1",
  treatmentConsentWitness2: "consentimientoTratamientoTestigo2",
  contractText: "contratoServiciosOdontologicosTexto",
  contractCity: "contratoServiciosCiudad",
  contractDate: "contratoServiciosFecha",
  contractPatient: "contratoServiciosPaciente",
  declarationDate: "declaracionVeracidadFecha",
  declarationPatientSignature: "firmaDeclaracionPaciente",
  declarationDoctorSignature: "firmaDeclaracionDoctor",
  treatmentConsentPatientSignature: "firmaConsentimientoTratamientoPaciente",
  treatmentConsentDoctorSignature: "firmaConsentimientoTratamientoDoctor",
  contractPatientSignature: "firmaContratoPaciente",
  contractDoctorSignature: "firmaContratoDoctor"
} as const;

export const APP_INFO_CONSENT_FIELD_IDS = {
  text: "consentimientoAppInformacionTexto",
  signerName: "consentimientoAppFirmante",
  relationship: "consentimientoAppCaracter",
  date: "consentimientoAppFecha",
  noticeVersion: "consentimientoAppAvisoPrivacidad",
  patientSignature: "firmaConsentimientoAppPaciente",
  doctorSignature: "firmaConsentimientoAppClinica"
} as const;

export const DEFAULT_APP_INFO_CONSENT_TEXT =
  "Declaro que he sido informado de que la clinica utiliza la plataforma Clinerya para integrar, conservar, consultar y administrar mi historia clinica odontologica y documentos relacionados. Autorizo el registro y tratamiento de mis datos personales, datos personales sensibles de salud, imagenes, firmas, documentos y demas informacion necesaria para la prestacion de servicios odontologicos, seguimiento clinico, comunicacion administrativa, facturacion, cumplimiento normativo y conservacion del expediente. Entiendo que la informacion sera tratada por la clinica y su personal autorizado, y que podra almacenarse y procesarse mediante herramientas tecnologicas de la app bajo medidas de seguridad administrativas, tecnicas y fisicas. Reconozco que puedo ejercer los derechos que correspondan conforme al aviso de privacidad de la clinica.";

function tableRows(rows: string[][]) {
  return JSON.stringify(rows);
}

const familyConditions = [
  "Diabetes",
  "Hipertension arterial",
  "Cardiopatias",
  "Neoplasias",
  "Epilepsia",
  "Malformaciones",
  "SIDA",
  "Enfermedades renales",
  "Hepatitis",
  "Artritis",
  "Enfermedad de transmision sexual",
  "Disfuncion endocrina",
  "Asma",
  "Fiebre reumatica",
  "Tuberculosis",
  "Traumatismo/secuelas",
  "Intervencion quirurgica",
  "Transfusion sanguinea",
  "Alergias",
  "Otra"
];

const habitRows = [
  "Respira por la boca",
  "Rechina los dientes",
  "Aprieta los dientes",
  "Muerde los lapices",
  "Muerde los labios",
  "Se chupa el dedo",
  "Se muerde las unas",
  "Succiona objetos",
  "Otro habito"
];

const pathologicalRows = [
  "Fuma",
  "Bebidas alcoholicas",
  "Drogas psicotropicas",
  "Ha padecido alguna enfermedad en alguna epoca",
  "Recibio tratamiento",
  "Ha sido hospitalizado",
  "Ha tomado algun medicamento para la circulacion"
];

const currentConditionRows = [
  "Presenta dolor o molestia en su boca",
  "Padece alguna enfermedad",
  "Se encuentra bajo algun tratamiento medico",
  "Toma algun medicamento",
  "Toma aspirina, analgesico o medicamento con frecuencia",
  "Fue prescrito algun medicamento"
];

const cardiovascularRows = [
  "Le duele la cabeza frecuentemente",
  "Tiene zumbido de oidos",
  "Se fatiga facilmente",
  "Le falta el aire en pequenos esfuerzos",
  "Le falta el aire en medianos esfuerzos",
  "Le falta el aire en grandes esfuerzos",
  "Ha sufrido desvanecimiento o desmayos",
  "Siente que se le borra la vista",
  "Se le hinchan los pies y manos",
  "Sufre de palpitaciones",
  "Tiene varices",
  "Otros datos no mencionados"
];

const endocrineRows = [
  "Se le seca la boca constantemente",
  "Tiene mucha sed",
  "Cuantas veces orina al dia",
  "Se levanta de noche varias veces a orinar",
  "Se fatiga facilmente",
  "Su piel presenta pigmentaciones",
  "Siente necesidad de ingerir salado o dulce"
];

const digestiveRows = [
  "Mastica bien sus alimentos",
  "Tolera bien sus alimentos",
  "Se le acumula alimento entre los dientes",
  "Tiene acidez",
  "Tiene dolor en la boca del estomago",
  "Defeca con facilidad",
  "Se le regresa la comida o reflujo",
  "Ha presentado sangrado por la boca",
  "Ha evacuado con sangre",
  "Es estrenido",
  "Evacua con facilidad",
  "Presenta inflamacion abdominal durante la digestion",
  "Presenta pirosis despues de comer",
  "Presenta dolor abdominal despues de ingerir algun alimento",
  "Irritantes, grasas, chocolate, cafe, cigarros u otros"
];

const respiratoryRows = [
  "Se siente cansado o fatigado con frecuencia",
  "Tiene tos seca",
  "Le duele la cabeza frecuentemente",
  "Tiene accesos de tos",
  "Siente que le falta el aire cuando se recuesta",
  "Tiene tos persistente",
  "Presenta secrecion nasal muy frecuente",
  "Tos en la manana",
  "Falta la respiracion cuando hace algun esfuerzo o ejercicio",
  "Silba el pecho cuando tiene accesos de tos",
  "Se le reseca la nariz",
  "Ha sufrido desmayo o desvanecimiento",
  "Se le reseca la boca",
  "Le falta el aire sin causa aparente",
  "Otros datos no mencionados"
];

const immunologicRows = [
  "Ronchas en la piel",
  "Inflamacion en la cara",
  "Escurrimiento nasal",
  "Lagrimeo",
  "Ojos rojos",
  "Ardor de ojos",
  "Comezon",
  "Tos",
  "Estornudos por la manana",
  "Otros datos no mencionados"
];

const musculoskeletalRows = [
  "Presenta dolor en las articulaciones",
  "Presenta dolor en la cadera, espalda o cintura",
  "Siente limitacion en sus movimientos",
  "Tiene dificultad para subir escaleras",
  "Siente sus musculos rigidos",
  "Le truenan las articulaciones",
  "Otros datos no mencionados"
];

const nervousRows = [
  "Se considera irritable",
  "Tiene ansiedad",
  "Se siente deprimido",
  "Se siente cansado durante el dia",
  "Ha sufrido convulsiones",
  "Le sudan las manos",
  "Tiene dificultad para dormir",
  "Se le duermen u hormiguean los brazos y manos",
  "Sufre constantes dolores de cabeza",
  "Le duele la mitad de la cabeza",
  "Le lastima la luz cuando le duele la cabeza",
  "El dolor de cabeza se desencadena con algun estimulo",
  "Presenta nauseas con el dolor de cabeza"
];

const lymphGenitourinaryRows = [
  "Le molesta el ruido cuando le duele la cabeza",
  "Ha sentido bolitas en cuello, axilas o ingles",
  "Siente ardor al orinar",
  "Siente dolor al orinar",
  "Tiene dificultad para orinar",
  "La orina es cristalina",
  "La orina es amarilla",
  "El olor de la orina es muy penetrante",
  "Esta bajo algun tratamiento hormonal",
  "Esta embarazada",
  "Si esta embarazada, cuantos meses tiene",
  "Utiliza algun metodo anticonceptivo",
  "Otros datos no mencionados"
];

export const ODONTOLOGY_TEMPLATE_DEFAULT_ANSWERS: Record<string, string> = {
  [ODONTOLOGY_FIELD_IDS.objectAndConfidentiality]:
    "Objeto: conocer el estado de salud del paciente, identificar indicaciones o contraindicaciones para la atencion odontologica y registrar informacion clinica necesaria. Toda la informacion recopilada es confidencial y se utilizara para fines de atencion, seguimiento y cumplimiento sanitario.",
  [ODONTOLOGY_FIELD_IDS.familyHistory]: tableRows(
    familyConditions.map((condition) => [condition, "", "", "", "", "", "", "", "", "", "", ""])
  ),
  [ODONTOLOGY_FIELD_IDS.immunizations]: tableRows([
    ["Hepatitis B", "", "", "", ""],
    ["Tetano / difteria / tosferina", "", "", "", ""],
    ["Sarampion / rubeola / parotiditis", "", "", "", ""],
    ["Tifoidea", "", "", "", ""],
    ["Poliomielitis", "", "", "", ""],
    ["Influenza", "", "", "", ""],
    ["Otra", "", "", "", ""]
  ]),
  [ODONTOLOGY_FIELD_IDS.habits]: tableRows(habitRows.map((habit) => [habit, "", ""])),
  [ODONTOLOGY_FIELD_IDS.diet]: tableRows([
    ["Desayuno", ""],
    ["Comida", ""],
    ["Cena", ""],
    ["Entre comidas", ""]
  ]),
  [ODONTOLOGY_FIELD_IDS.pathologicalHistory]: tableRows(pathologicalRows.map((item) => [item, "", "", ""])),
  [ODONTOLOGY_FIELD_IDS.currentCondition]: tableRows(currentConditionRows.map((item) => [item, "", ""])),
  [ODONTOLOGY_FIELD_IDS.cardiovascular]: tableRows(cardiovascularRows.map((item) => [item, "", ""])),
  [ODONTOLOGY_FIELD_IDS.endocrine]: tableRows(endocrineRows.map((item) => [item, "", ""])),
  [ODONTOLOGY_FIELD_IDS.digestive]: tableRows(digestiveRows.map((item) => [item, "", ""])),
  [ODONTOLOGY_FIELD_IDS.respiratory]: tableRows(respiratoryRows.map((item) => [item, "", ""])),
  [ODONTOLOGY_FIELD_IDS.immunologic]: tableRows(immunologicRows.map((item) => [item, "", ""])),
  [ODONTOLOGY_FIELD_IDS.musculoskeletal]: tableRows(musculoskeletalRows.map((item) => [item, "", ""])),
  [ODONTOLOGY_FIELD_IDS.nervous]: tableRows(nervousRows.map((item) => [item, "", ""])),
  [ODONTOLOGY_FIELD_IDS.lymphGenitourinary]: tableRows(lymphGenitourinaryRows.map((item) => [item, "", ""])),
  [ODONTOLOGY_FIELD_IDS.truthStatement]:
    "Hago constar que todos los datos que he proporcionado son veridicos y me comprometo a informar inmediatamente cualquier cambio en mi estado de salud.",
  [ODONTOLOGY_FIELD_IDS.cpod]: tableRows([
    ["Cariados", "", ""],
    ["Perdidos", "", ""],
    ["Obturados", "", ""],
    ["Total CPOD", "", ""]
  ]),
  [ODONTOLOGY_FIELD_IDS.treatmentConsentText]:
    "Por medio del presente documento, en mi nombre propio o en pleno uso de mis facultades mentales, otorgo mi consentimiento libre, informado y voluntario al odontologo tratante, asi como a los auxiliares y personal de apoyo autorizado, para la realizacion del tratamiento indicado. Reconozco que se me han explicado el diagnostico, pronostico, alternativas, anestesia, beneficios, molestias, limitaciones y posibles riesgos generales y especificos. Entiendo que el resultado del tratamiento no depende exclusivamente del odontologo y que debo seguir las indicaciones otorgadas. Declaro que la informacion proporcionada sobre mi estado de salud es cierta y que he tenido oportunidad de resolver mis dudas antes de firmar.",
  [ODONTOLOGY_FIELD_IDS.contractText]:
    "Contrato de adhesion para la prestacion de servicios odontologicos que celebran por una parte la clinica o prestador de servicios odontologicos y por la otra el usuario o paciente cuyos datos constan en la historia clinica. El prestador declara contar con la capacidad profesional, instalaciones y autorizaciones necesarias para ofrecer servicios odontologicos. El usuario declara haber recibido informacion sobre precios, diagnosticos, analisis, tratamientos, presupuestos y condiciones de atencion. Las partes acuerdan que el objeto del contrato es la prestacion de servicios odontologicos solicitados por el usuario; que el usuario proporcionara datos de contacto y notificara cambios de cita con anticipacion; que los costos, formas de pago, vigencia del presupuesto, interconsultas, seguimiento, indicaciones posteriores y revisiones se sujetaran al presupuesto aceptado, a la evolucion del caso clinico y a las politicas informadas por la clinica.",
  [APP_INFO_CONSENT_FIELD_IDS.text]: DEFAULT_APP_INFO_CONSENT_TEXT,
  [APP_INFO_CONSENT_FIELD_IDS.noticeVersion]: "Aviso de privacidad vigente de la clinica"
};

interface FieldSpec {
  id: string;
  label: string;
  type?: TemplateElement["type"];
  sectionId?: string;
  options?: string[];
  columns?: string[];
  height?: number;
  fontSize?: number;
  bold?: boolean;
  align?: TemplateElement["align"];
}

function createField(spec: FieldSpec, x: number, y: number, width: number, height: number): TemplateElement {
  return {
    id: spec.id,
    sectionId: spec.sectionId,
    label: spec.label,
    type: spec.type ?? "text",
    options: spec.options,
    columns: spec.columns,
    x,
    y,
    width,
    height,
    fontSize: spec.fontSize,
    bold: spec.bold,
    align: spec.align
  };
}

function addFull(elements: TemplateElement[], y: number, spec: FieldSpec): number {
  const height = spec.height ?? 80;
  elements.push(createField(spec, ELEMENT_MARGIN, y, FULL_WIDTH, height));
  return y + height + ROW_GAP;
}

function addRow(elements: TemplateElement[], y: number, specs: FieldSpec[], height = 56): number {
  const count = specs.length;
  const width = count === 3 ? THIRD_WIDTH : Math.floor((FULL_WIDTH - GUTTER * (count - 1)) / count);
  specs.forEach((spec, index) => {
    elements.push(createField(spec, ELEMENT_MARGIN + index * (width + GUTTER), y, width, spec.height ?? height));
  });
  return y + Math.max(...specs.map((spec) => spec.height ?? height)) + ROW_GAP;
}

function withHeaderSpace(page: TemplatePage): TemplatePage {
  return {
    ...page,
    elements: [
      ...page.elements,
      {
        id: "clinicHeaderTop",
        label: "Logotipo y datos de la clinica",
        type: "clinic_header",
        x: ELEMENT_MARGIN,
        y: ELEMENT_MARGIN,
        width: 320,
        height: HEADER_HEIGHT
      },
      {
        id: "clinicHeaderFooter",
        label: "Logotipo y datos de la clinica",
        type: "clinic_header",
        x: CANVAS_WIDTH - ELEMENT_MARGIN - 320,
        y: page.canvasHeight - ELEMENT_MARGIN - HEADER_HEIGHT,
        width: 320,
        height: HEADER_HEIGHT
      }
    ]
  };
}

function page(elements: TemplateElement[]): TemplatePage {
  return { id: genPageId(), elements, canvasHeight: CANVAS_PAGE_HEIGHT };
}

function buildIdentificationPage(): TemplatePage {
  const elements: TemplateElement[] = [];
  let y = ELEMENT_MARGIN + HEADER_OFFSET;

  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.objectAndConfidentiality,
    sectionId: SECTIONS.identification,
    label: "Objeto y confidencialidad",
    type: "textarea",
    height: 84,
    align: "left"
  });
  y = addRow(elements, y, [
    { id: "fechaHistoriaDental", sectionId: SECTIONS.identification, label: "Fecha", type: "date" },
    { id: "nombreTutor", sectionId: SECTIONS.identification, label: "Nombre completo del tutor" }
  ]);
  y = addRow(elements, y, [
    { id: "nombre", sectionId: SECTIONS.identification, label: "Nombre completo del paciente" },
    { id: "edad", sectionId: SECTIONS.identification, label: "Edad", type: "number" },
    { id: "sexo", sectionId: SECTIONS.identification, label: "Sexo", type: "select", options: ["Masculino", "Femenino"] }
  ]);
  y = addRow(elements, y, [
    { id: "fechaNacimiento", sectionId: SECTIONS.identification, label: "Fecha de nacimiento", type: "date" },
    { id: "estadoCivil", sectionId: SECTIONS.identification, label: "Estado civil" },
    { id: "ocupacion", sectionId: SECTIONS.identification, label: "Ocupacion" }
  ]);
  y = addFull(elements, y, { id: "domicilio", sectionId: SECTIONS.identification, label: "Domicilio", height: 56 });
  y = addRow(elements, y, [
    { id: "telefonoCasa", sectionId: SECTIONS.identification, label: "Telefono casa" },
    { id: "telefono", sectionId: SECTIONS.identification, label: "Celular" },
    { id: "telefonoOtro", sectionId: SECTIONS.identification, label: "Otro telefono" }
  ]);
  y = addRow(elements, y, [
    { id: "email", sectionId: SECTIONS.identification, label: "E-mail" },
    { id: "recomendadoPor", sectionId: SECTIONS.identification, label: "Recomendado por" }
  ]);
  y = addRow(elements, y, [
    { id: "contactoEmergencia", sectionId: SECTIONS.identification, label: "En caso de emergencia avisar a" },
    { id: "telefonoEmergencia", sectionId: SECTIONS.identification, label: "Telefono de emergencia" },
    { id: "celularEmergencia", sectionId: SECTIONS.identification, label: "Celular de emergencia" }
  ]);
  y = addFull(elements, y, {
    id: "motivoConsultaDental",
    sectionId: SECTIONS.current,
    label: "Motivo de la consulta",
    type: "textarea",
    height: 90
  });
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.familyHistory,
    sectionId: SECTIONS.family,
    label: "Antecedentes heredofamiliares",
    type: "table",
    columns: ["Patologia", "Madre", "Abuela M", "Abuelo M", "Otros M", "Padre", "Abuela P", "Abuelo P", "Otros P", "Hermano/a", "Paciente", "Observaciones"],
    height: 230
  });
  addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.immunizations,
    sectionId: SECTIONS.vaccination,
    label: "Inmunizaciones",
    type: "table",
    columns: ["Vacuna", "Esquema completo", "Incompleto en proceso", "Ninguna dosis", "Observaciones"],
    height: 160
  });

  return withHeaderSpace(page(elements));
}

function buildHabitsAndPathologyPage(): TemplatePage {
  const elements: TemplateElement[] = [];
  let y = ELEMENT_MARGIN;

  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.habits,
    sectionId: SECTIONS.nonPathological,
    label: "Habitos",
    type: "table",
    columns: ["Habito", "Si/No", "Observaciones"],
    height: 180
  });
  y = addRow(elements, y, [
    { id: "cepilladoDentalFrecuencia", sectionId: SECTIONS.nonPathological, label: "Cuantas veces al dia se cepilla los dientes" },
    { id: "ejercicioDental", sectionId: SECTIONS.nonPathological, label: "Acostumbra hacer ejercicio", type: "select", options: YES_NO_OPTIONS },
    { id: "bebidasEnergeticasDental", sectionId: SECTIONS.nonPathological, label: "Ingiere bebidas energetizantes", type: "select", options: YES_NO_OPTIONS }
  ]);
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.diet,
    sectionId: SECTIONS.nonPathological,
    label: "Alimentacion",
    type: "table",
    columns: ["Tiempo de comida", "Alimentos que acostumbra ingerir"],
    height: 150
  });
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.pathologicalHistory,
    sectionId: SECTIONS.pathological,
    label: "Antecedentes personales patologicos",
    type: "table",
    columns: ["Antecedente", "Si/No", "Cantidad / tipo / cual", "Tiempo / observaciones"],
    height: 210
  });
  addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.currentCondition,
    sectionId: SECTIONS.current,
    label: "Padecimiento actual",
    type: "table",
    columns: ["Pregunta", "Si/No", "Mencione / observaciones"],
    height: 190
  });

  return page(elements);
}

function buildSystemsPageOne(): TemplatePage {
  const elements: TemplateElement[] = [];
  let y = ELEMENT_MARGIN;

  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.cardiovascular,
    sectionId: SECTIONS.systems,
    label: "Aparatos y sistemas - Cardiovascular",
    type: "table",
    columns: ["Dato clinico", "Si/No", "Observaciones"],
    height: 240
  });
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.endocrine,
    sectionId: SECTIONS.systems,
    label: "Aparatos y sistemas - Endocrino",
    type: "table",
    columns: ["Dato clinico", "Si/No", "Observaciones"],
    height: 170
  });
  y = addFull(elements, y, {
    id: "sintomasAutonomicosDental",
    sectionId: SECTIONS.systems,
    label: "Mareos, sudoracion fria, desvanecimiento, palpitaciones, palidez, nauseas o hambre",
    type: "textarea",
    height: 90
  });
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.digestive,
    sectionId: SECTIONS.systems,
    label: "Aparatos y sistemas - Sistema digestivo",
    type: "table",
    columns: ["Dato clinico", "Si/No", "Observaciones"],
    height: 250
  });
  addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.respiratory,
    sectionId: SECTIONS.systems,
    label: "Aparatos y sistemas - Sistema respiratorio",
    type: "table",
    columns: ["Dato clinico", "Si/No", "Observaciones"],
    height: 230
  });

  return page(elements);
}

function buildSystemsPageTwo(): TemplatePage {
  const elements: TemplateElement[] = [];
  let y = ELEMENT_MARGIN;

  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.immunologic,
    sectionId: SECTIONS.systems,
    label: "Aparatos y sistemas - Sistema inmunologico",
    type: "table",
    columns: ["Signo o sintoma", "Si/No", "Observaciones"],
    height: 190
  });
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.musculoskeletal,
    sectionId: SECTIONS.systems,
    label: "Aparatos y sistemas - Sistema musculo esqueletico",
    type: "table",
    columns: ["Dato clinico", "Si/No", "Observaciones"],
    height: 190
  });
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.nervous,
    sectionId: SECTIONS.systems,
    label: "Aparatos y sistemas - Sistema nervioso",
    type: "table",
    columns: ["Dato clinico", "Si/No", "Observaciones"],
    height: 240
  });

  return page(elements);
}

function buildClinicalAndOdontogramPage(): TemplatePage {
  const elements: TemplateElement[] = [];
  let y = ELEMENT_MARGIN;

  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.lymphGenitourinary,
    sectionId: SECTIONS.systems,
    label: "Aparatos y sistemas - Linfo-hematico y genitourinario",
    type: "table",
    columns: ["Dato clinico", "Si/No", "Observaciones"],
    height: 210
  });
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.additionalHealthInfo,
    sectionId: SECTIONS.systems,
    label: "Otra informacion sobre su estado de salud no mencionada anteriormente",
    type: "textarea",
    height: 85
  });
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.truthStatement,
    sectionId: SECTIONS.current,
    label: "Declaracion de veracidad",
    type: "textarea",
    height: 80
  });
  y = addRow(elements, y, [
    { id: ODONTOLOGY_FIELD_IDS.declarationDate, label: "Fecha", type: "date" },
    { id: ODONTOLOGY_FIELD_IDS.declarationPatientSignature, label: "Nombre y firma del paciente", type: "signature_patient", height: 120 },
    { id: ODONTOLOGY_FIELD_IDS.declarationDoctorSignature, label: "C.D.", type: "signature_doctor", height: 120 }
  ], 120);
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.odontogram,
    label: "Odontograma",
    type: "odontogram",
    height: defaultSizeForType("odontogram").height
  });
  y = addRow(elements, y, [
    {
      id: ODONTOLOGY_FIELD_IDS.cpod,
      label: "Indice CPOD",
      type: "table",
      columns: ["Indicador", "Inicial", "Final"],
      height: 150
    },
    {
      id: ODONTOLOGY_FIELD_IDS.odontogramNotes,
      label: "Observaciones del odontograma",
      type: "textarea",
      height: 150
    }
  ], 150);

  return page(elements);
}

function buildTreatmentConsentPage(): TemplatePage {
  const elements: TemplateElement[] = [];
  let y = ELEMENT_MARGIN;

  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.treatmentConsentText,
    sectionId: CONSENT_SECTION_ID,
    label: "Consentimiento informado para tratamientos odontologicos, intervenciones quirurgicas y procedimientos especiales",
    type: "textarea",
    height: 300,
    align: "left",
    fontSize: 11
  });
  y = addRow(elements, y, [
    { id: ODONTOLOGY_FIELD_IDS.treatmentConsentPatient, sectionId: CONSENT_SECTION_ID, label: "Nombre del paciente" },
    { id: ODONTOLOGY_FIELD_IDS.treatmentConsentAge, sectionId: CONSENT_SECTION_ID, label: "Edad", type: "number" },
    { id: ODONTOLOGY_FIELD_IDS.treatmentConsentDate, sectionId: CONSENT_SECTION_ID, label: "Fecha", type: "date" }
  ]);
  y = addRow(elements, y, [
    { id: ODONTOLOGY_FIELD_IDS.treatmentConsentCity, sectionId: CONSENT_SECTION_ID, label: "Ciudad" },
    { id: ODONTOLOGY_FIELD_IDS.treatmentConsentPrognosis, sectionId: CONSENT_SECTION_ID, label: "Pronostico", type: "select", options: ["Bueno", "Regular", "Malo"] }
  ]);
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.treatmentConsentProcedure,
    sectionId: CONSENT_SECTION_ID,
    label: "Tratamiento o procedimiento autorizado",
    type: "textarea",
    height: 90
  });
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.treatmentConsentGeneralRisks,
    sectionId: CONSENT_SECTION_ID,
    label: "Riesgos generales",
    type: "textarea",
    height: 85
  });
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.treatmentConsentSpecificRisks,
    sectionId: CONSENT_SECTION_ID,
    label: "Riesgos especificos",
    type: "textarea",
    height: 85
  });
  y = addRow(elements, y, [
    { id: ODONTOLOGY_FIELD_IDS.treatmentConsentPatientSignature, sectionId: CONSENT_SECTION_ID, label: "Nombre y firma del paciente", type: "signature_patient", height: 120 },
    { id: ODONTOLOGY_FIELD_IDS.treatmentConsentDoctorSignature, sectionId: CONSENT_SECTION_ID, label: "C.D.", type: "signature_doctor", height: 120 }
  ], 120);
  addRow(elements, y, [
    { id: ODONTOLOGY_FIELD_IDS.treatmentConsentWitness1, sectionId: CONSENT_SECTION_ID, label: "Nombre y firma del testigo 1" },
    { id: ODONTOLOGY_FIELD_IDS.treatmentConsentWitness2, sectionId: CONSENT_SECTION_ID, label: "Nombre y firma del testigo 2" }
  ]);

  return page(elements);
}

function buildServiceContractPage(): TemplatePage {
  const elements: TemplateElement[] = [];
  let y = ELEMENT_MARGIN;

  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.contractText,
    label: "Contrato de adhesion para la prestacion de servicios odontologicos",
    type: "textarea",
    height: 520,
    align: "left",
    fontSize: 10
  });
  y = addRow(elements, y, [
    { id: ODONTOLOGY_FIELD_IDS.contractCity, label: "Ciudad" },
    { id: ODONTOLOGY_FIELD_IDS.contractDate, label: "Fecha", type: "date" }
  ]);
  y = addFull(elements, y, {
    id: ODONTOLOGY_FIELD_IDS.contractPatient,
    label: "Nombre del paciente o usuario",
    height: 56
  });
  addRow(elements, y, [
    { id: ODONTOLOGY_FIELD_IDS.contractPatientSignature, label: "Nombre y firma del paciente", type: "signature_patient", height: 120 },
    { id: ODONTOLOGY_FIELD_IDS.contractDoctorSignature, label: "C.D.", type: "signature_doctor", height: 120 }
  ], 120);

  return page(elements);
}

function buildAppInfoConsentPage(): TemplatePage {
  const elements: TemplateElement[] = [];
  let y = ELEMENT_MARGIN;

  y = addFull(elements, y, {
    id: APP_INFO_CONSENT_FIELD_IDS.text,
    sectionId: CONSENT_SECTION_ID,
    label: "Consentimiento informado de uso de la app y tratamiento de informacion",
    type: "textarea",
    height: 430,
    align: "left",
    fontSize: 11
  });
  y = addRow(elements, y, [
    { id: APP_INFO_CONSENT_FIELD_IDS.signerName, sectionId: CONSENT_SECTION_ID, label: "Nombre del paciente o representante legal" },
    { id: APP_INFO_CONSENT_FIELD_IDS.relationship, sectionId: CONSENT_SECTION_ID, label: "Caracter con el que firma" }
  ]);
  y = addRow(elements, y, [
    { id: APP_INFO_CONSENT_FIELD_IDS.date, sectionId: CONSENT_SECTION_ID, label: "Fecha", type: "date" },
    { id: APP_INFO_CONSENT_FIELD_IDS.noticeVersion, sectionId: CONSENT_SECTION_ID, label: "Aviso de privacidad o version informada" }
  ]);
  addRow(elements, y + 40, [
    { id: APP_INFO_CONSENT_FIELD_IDS.patientSignature, sectionId: CONSENT_SECTION_ID, label: "Firma del paciente o representante legal", type: "signature_patient", height: 120 },
    { id: APP_INFO_CONSENT_FIELD_IDS.doctorSignature, sectionId: CONSENT_SECTION_ID, label: "Firma del responsable de la clinica", type: "signature_doctor", height: 120 }
  ], 120);

  return page(elements);
}

export function buildNomOdontologyStarterPages(): TemplatePage[] {
  return [
    buildIdentificationPage(),
    buildHabitsAndPathologyPage(),
    buildSystemsPageOne(),
    buildSystemsPageTwo(),
    buildClinicalAndOdontogramPage(),
    buildTreatmentConsentPage(),
    buildAppInfoConsentPage(),
    buildServiceContractPage()
  ];
}
