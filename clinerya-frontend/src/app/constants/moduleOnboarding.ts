import type { ModuleKey } from "./modules";

export type ModuleOnboardingCopy = {
  eyebrow: string;
  title: string;
  description: string;
  highlights: string[];
  setupSteps: string[];
  primaryLabel: string;
};

export const moduleOnboardingCopy: Record<ModuleKey, ModuleOnboardingCopy> = {
  dashboard: {
    eyebrow: "Inicio",
    title: "Observa la clinica de un vistazo",
    description:
      "Dashboard resume los modulos principales, el estado de configuracion y los accesos rapidos para continuar el trabajo.",
    highlights: ["Resumen por modulo", "Indicadores operativos", "Accesos directos al flujo de configuracion"],
    setupSteps: ["Revisa el estado de la clinica", "Entra a los modulos pendientes", "Continua la operacion desde los accesos rapidos"],
    primaryLabel: "Ir al Dashboard"
  },
  agenda: {
    eyebrow: "Primera vez en Agenda",
    title: "Prepara la operacion diaria de tu clinica",
    description:
      "Agenda concentra horarios, doctores, citas y seguimiento del dia para que el equipo trabaje con una sola vista.",
    highlights: ["Vista semanal y por lista", "Citas con paciente nuevo o registrado", "Conexiones con caja e inventario"],
    setupSteps: ["Revisa los horarios de atencion", "Confirma los doctores disponibles", "Crea o importa la primera cita"],
    primaryLabel: "Comenzar en Agenda"
  },
  solicitudes: {
    eyebrow: "Primera vez en Solicitudes de cita",
    title: "Responde las citas que piden tus pacientes por WhatsApp",
    description:
      "El asistente de WhatsApp aparta el horario que eligió el paciente y te lo trae aquí. Solo tú, como su médico, la aceptas, la rechazas o propones otros horarios.",
    highlights: ["Se actualiza en vivo", "El horario queda apartado 24 h", "Propón hasta 3 horarios"],
    setupSteps: ["Registra tu celular en Perfil para recibir avisos", "Acepta o propón horarios", "El paciente recibe la respuesta por WhatsApp"],
    primaryLabel: "Ver solicitudes"
  },
  chats: {
    eyebrow: "Primera vez en Chats de WhatsApp",
    title: "Consulta lo que el asistente habló con cada paciente",
    description:
      "Aquí están las conversaciones del asistente de WhatsApp. Son de solo lectura y cada vez que alguien abre un chat queda registrado.",
    highlights: ["Actividad en vivo", "Solo lectura", "Cada lectura queda auditada"],
    setupSteps: ["Elige un chat de la lista", "Lee la conversación", "Aprueba las citas en Solicitudes de cita"],
    primaryLabel: "Ver chats"
  },
  consultorios: {
    eyebrow: "Primera vez en Consultorios",
    title: "Organiza los espacios de atencion de tu clinica",
    description: "Crea consultorios, revisa sus propiedades y manten su estado disponible para la Agenda.",
    highlights: ["Resumen por consultorio", "Alta de nuevos espacios", "Estado activo e inactivo"],
    setupSteps: ["Crea tu primer consultorio", "Revisa sus propiedades", "Usalo al agendar una cita"],
    primaryLabel: "Abrir Consultorios"
  },
  atencion: {
    eyebrow: "Atención al Paciente",
    title: "Maneja la consulta médica en tiempo real",
    description:
      "Atención al Paciente reúne historia clínica, notas SOAP, recetas médicas y comparativo de insumos consumidos en una sola pantalla.",
    highlights: ["Acceso e inicio de Historia Clínica", "Notas médicas formato SOAP", "Emisión e impresión de recetas", "Consumo real vs planeado de materiales"],
    setupSteps: ["Selecciona un paciente o cita activa", "Verifica la historia clínica inicial", "Registra la nota y emite recetas"],
    primaryLabel: "Comenzar Atención"
  },
  pacientes: {
    eyebrow: "Primera vez en Pacientes",
    title: "Construye el directorio clinico desde el primer registro",
    description:
      "Pacientes guarda datos de contacto, informacion fiscal y contexto administrativo para alimentar expediente, citas y cobros.",
    highlights: ["Busqueda rapida por nombre o CURP", "Datos fiscales y de contacto", "Base compartida para todos los flujos"],
    setupSteps: ["Registra tu primer paciente", "Completa datos de contacto", "Valida que aparezca en agenda y expediente"],
    primaryLabel: "Comenzar en Pacientes"
  },
  expediente: {
    eyebrow: "Primera vez en Expediente",
    title: "Deja listas las historias clinicas de la consulta",
    description:
      "Expediente organiza plantillas, notas clinicas y documentos para que cada paciente tenga su historia completa.",
    highlights: ["Plantillas clinicas reutilizables", "Notas y documentos por paciente", "Acceso ordenado desde el directorio"],
    setupSteps: ["Elige un paciente de referencia", "Prepara una plantilla inicial", "Captura la primera nota clinica"],
    primaryLabel: "Comenzar en Expediente"
  },
  tratamientos: {
    eyebrow: "Primera vez en Tratamientos",
    title: "Configura el catalogo que sostiene tus cotizaciones",
    description:
      "Tratamientos conecta servicios, precios, materiales y cotizaciones para registrar presupuestos y visitas con claridad.",
    highlights: ["Catalogo de servicios", "Cotizaciones por paciente", "Relacion con materiales de inventario"],
    setupSteps: ["Crea servicios frecuentes", "Define precios y materiales", "Genera la primera cotizacion"],
    primaryLabel: "Comenzar en Tratamientos"
  },
  caja: {
    eyebrow: "Primera vez en Caja",
    title: "Prepara cobros, turnos y tickets antes de operar",
    description:
      "Caja concentra aperturas de turno, pagos, gastos y tickets para mantener el dinero del dia bajo control.",
    highlights: ["Apertura y cierre de caja", "Cobros ligados a citas o cotizaciones", "Tickets y movimientos administrativos"],
    setupSteps: ["Abre el primer turno de caja", "Selecciona responsable", "Registra el primer cobro o gasto"],
    primaryLabel: "Comenzar en Caja"
  },
  inventario: {
    eyebrow: "Primera vez en Inventario",
    title: "Ordena materiales, lotes y existencias desde el inicio",
    description:
      "Inventario ayuda a controlar insumos clinicos, movimientos, caducidades y consumo asociado a tratamientos.",
    highlights: ["Catalogo de materiales", "Entradas, salidas y ajustes", "Lotes y existencias actualizadas"],
    setupSteps: ["Crea los materiales principales", "Define unidades y stock minimo", "Registra una entrada inicial"],
    primaryLabel: "Comenzar en Inventario"
  },
  contabilidad: {
    eyebrow: "Primera vez en Contabilidad",
    title: "Abre la contabilidad con saldos iniciales",
    description:
      "Contabilidad automatiza polizas, bancos, balanza y resultados a partir de la operacion de caja e inventario.",
    highlights: ["Libro diario automatizado", "Cuentas bancarias", "Balanza y estado de resultados"],
    setupSteps: ["Define fecha de apertura", "Captura caja, bancos e inventario", "Registra la poliza de capital inicial"],
    primaryLabel: "Configurar saldos iniciales"
  },
  personal: {
    eyebrow: "Primera vez en Personal",
    title: "Controla accesos y colaboracion alrededor del paciente",
    description:
      "Personal permite revisar accesos externos y compartir expedientes con especialistas cuando el caso lo requiere.",
    highlights: ["Accesos por paciente", "Especialistas externos", "Seguimiento de permisos compartidos"],
    setupSteps: ["Revisa los accesos activos", "Invita a un especialista si aplica", "Confirma permisos del expediente"],
    primaryLabel: "Comenzar en Personal"
  },
  configuracion: {
    eyebrow: "Configuracion General",
    title: "Manten la clinica lista para operar",
    description: "Administra datos de la clinica, consultorios, integraciones y preferencias del sistema.",
    highlights: ["Propiedades de la clinica", "Consultorios asignados", "Integraciones y preferencias"],
    setupSteps: ["Revisa los datos de la clinica", "Crea los consultorios disponibles", "Configura tus preferencias"],
    primaryLabel: "Abrir Configuracion"
  }
};
