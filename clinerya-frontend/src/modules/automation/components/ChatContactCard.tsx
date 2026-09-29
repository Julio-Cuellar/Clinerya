import { useEffect, useRef, useState } from "react";
import { getFriendlyError, whatsAppChatsApi } from "@shared/api/api";
import type { ChatContact } from "../types";
import { chatPhone, phoneOrigin, signalLabel, visitsLine } from "../logic/contact";

const DATE = new Intl.DateTimeFormat("es-MX", { day: "numeric", month: "short", year: "numeric" });
const MONTH_YEAR = new Intl.DateTimeFormat("es-MX", { month: "long", year: "numeric" });
const DATE_TIME = new Intl.DateTimeFormat("es-MX", { weekday: "short", day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });

const fmt = (formatter: Intl.DateTimeFormat, value: string | null) => (value ? formatter.format(new Date(value)) : "—");

/**
 * Ficha breve de quien escribe: para reconocer al paciente o sospechar de spam sin salir del chat.
 * Solo identificacion y citas; nada del expediente. Las senales orientan, no bloquean.
 */
export function ChatContactCard({
  clinicId,
  phone,
  onClose,
  onOpenPatients,
  onRegisterPatient
}: {
  clinicId: string;
  phone: string;
  onClose: () => void;
  onOpenPatients?: () => void;
  onRegisterPatient?: () => void;
}) {
  const [contact, setContact] = useState<ChatContact | null>(null);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);
  const cardRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    let cancelled = false;
    whatsAppChatsApi
      .contact(clinicId, phone)
      .then((found) => !cancelled && setContact(found))
      .catch((caught) => !cancelled && setError(getFriendlyError(caught)));
    return () => {
      cancelled = true;
    };
  }, [clinicId, phone]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => event.key === "Escape" && onClose();
    const onClick = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      // El boton del nombre abre y cierra la ficha por su cuenta; cerrar aqui la reabriria con su clic.
      if (target.closest?.(".wa-name-button")) return;
      if (cardRef.current && !cardRef.current.contains(target)) onClose();
    };
    document.addEventListener("keydown", onKey);
    document.addEventListener("mousedown", onClick);
    cardRef.current?.focus();
    return () => {
      document.removeEventListener("keydown", onKey);
      document.removeEventListener("mousedown", onClick);
    };
  }, [onClose]);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(chatPhone(phone));
      setCopied(true);
    } catch {
      setError("No se pudo copiar el celular.");
    }
  };

  const registered = (contact?.patients.length ?? 0) > 0;

  return (
    <div className="wa-contact-card" role="dialog" aria-label="Ficha del contacto" ref={cardRef} tabIndex={-1}>
      <div className="wa-contact-head">
        <h3>{registered ? contact!.patients.map((patient) => patient.fullName).join(" · ") : "Número sin registrar"}</h3>
        <button type="button" className="wa-link" aria-label="Cerrar" onClick={onClose}>✕</button>
      </div>
      {error && <p className="alert error" style={{ margin: 0 }}>{error}</p>}
      {!contact && !error && <p className="wa-help" style={{ margin: 0 }}>Cargando…</p>}
      {contact && (
        <>
          <div className="wa-contact-flags">
            {registered && <span className="wa-pill ok">{contact.patients.length === 1 ? "Paciente registrado" : `${contact.patients.length} pacientes con este número`}</span>}
            {contact.signals.map((signal) => (
              <span key={signal} className={`wa-pill ${signal === "NEVER_HAD_APPOINTMENT" ? "info" : "warn"}`}>{signalLabel(signal)}</span>
            ))}
          </div>
          <dl className="wa-contact-data">
            <dt>Celular</dt>
            <dd className="wa-phone">{chatPhone(phone)} · {phoneOrigin(phone)}</dd>
            {contact.profileName && (<><dt>Perfil de WhatsApp</dt><dd>"{contact.profileName}"</dd></>)}
            <dt>Escribe desde</dt>
            <dd>{fmt(DATE, contact.firstMessageAt)} · {contact.messageCount} {contact.messageCount === 1 ? "mensaje" : "mensajes"}</dd>
            {!registered && (<><dt>Pacientes con este número</dt><dd>Ninguno</dd></>)}
          </dl>
          {contact.patients.map((patient) => (
            <dl key={patient.patientId} className="wa-contact-data">
              {contact.patients.length > 1 && (<><dt>Paciente</dt><dd><strong>{patient.fullName}</strong></dd></>)}
              <dt>Edad</dt><dd>{patient.age === null ? "—" : `${patient.age} años`}</dd>
              <dt>Paciente desde</dt><dd>{fmt(MONTH_YEAR, patient.registeredAt)}</dd>
              <dt>WhatsApp</dt><dd>{patient.whatsappConsent ? "Autorizó mensajes de la clínica" : "No ha autorizado mensajes"}</dd>
              <dt>Próxima cita</dt>
              <dd>{patient.nextStart ? `${fmt(DATE_TIME, patient.nextStart)}${patient.nextDoctorName ? ` · ${patient.nextDoctorName}` : ""}` : "Ninguna"}</dd>
              <dt>Última cita</dt>
              <dd>{patient.lastAttendedStart ? `${fmt(DATE, patient.lastAttendedStart)}${patient.lastAttendedDoctorName ? ` · ${patient.lastAttendedDoctorName}` : ""}` : "—"}</dd>
              <dt>Citas</dt><dd>{visitsLine(patient)}</dd>
            </dl>
          ))}
          <div className="wa-contact-actions">
            <button type="button" className="btn ghost" onClick={() => void copy()}>{copied ? "Copiado" : "Copiar celular"}</button>
            {registered
              ? onOpenPatients && <button type="button" className="btn primary" onClick={onOpenPatients}>Ir a Pacientes</button>
              : onRegisterPatient && <button type="button" className="btn primary" onClick={onRegisterPatient}>Registrar como paciente</button>}
          </div>
        </>
      )}
    </div>
  );
}
