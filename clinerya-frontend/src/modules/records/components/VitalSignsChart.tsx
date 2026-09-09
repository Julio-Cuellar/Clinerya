import { useEffect, useMemo, useState } from "react";
import { IconChartLine, IconChevronDown, IconChevronRight } from "@tabler/icons-react";
import { clinicalNotesApi, getFriendlyError } from "@shared/api/api";
import type { ClinicalNoteResponse } from "@modules/records/types";

interface Point {
  date: string;
  value: number;
}

interface CardSeries {
  color: string;
  legend?: string;
  points: Point[];
}

interface MetricCard {
  key: string;
  label: string;
  unit: string;
  series: CardSeries[];
}

const CARD_HEIGHT = 56;
const CARD_WIDTH = 260;
const PAD = 6;

function fmt(value: number): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(1);
}

function fmtDate(iso: string): string {
  return new Date(iso).toLocaleDateString("es-MX", { day: "2-digit", month: "short", year: "numeric" });
}

function parseBloodPressure(raw: string | null | undefined): [number, number] | null {
  if (!raw) return null;
  const parts = raw.split("/").map((part) => Number.parseInt(part.trim(), 10));
  if (parts.length !== 2 || Number.isNaN(parts[0]) || Number.isNaN(parts[1])) return null;
  return [parts[0], parts[1]];
}

function Sparkline({ series }: { series: CardSeries[] }) {
  const values = series.flatMap((line) => line.points.map((point) => point.value));
  const times = series.flatMap((line) => line.points.map((point) => new Date(point.date).getTime()));
  if (values.length === 0) return null;

  let min = Math.min(...values);
  let max = Math.max(...values);
  if (min === max) {
    min -= 1;
    max += 1;
  }
  const tMin = Math.min(...times);
  const tMax = Math.max(...times);

  const x = (time: number) =>
    tMax === tMin ? CARD_WIDTH / 2 : PAD + ((time - tMin) / (tMax - tMin)) * (CARD_WIDTH - PAD * 2);
  const y = (value: number) => PAD + (1 - (value - min) / (max - min)) * (CARD_HEIGHT - PAD * 2);

  return (
    <svg className="vitals-spark" viewBox={`0 0 ${CARD_WIDTH} ${CARD_HEIGHT}`} role="img" aria-hidden="true">
      {series.map((line, lineIndex) => {
        const coords = line.points
          .slice()
          .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
          .map((point) => `${x(new Date(point.date).getTime())},${y(point.value)}`)
          .join(" ");
        return (
          <g key={lineIndex}>
            {line.points.length > 1 && (
              <polyline
                points={coords}
                fill="none"
                stroke={line.color}
                strokeWidth={1.5}
                strokeLinejoin="round"
                strokeLinecap="round"
                vectorEffect="non-scaling-stroke"
              />
            )}
            {line.points.map((point, pointIndex) => (
              <circle
                key={pointIndex}
                cx={x(new Date(point.date).getTime())}
                cy={y(point.value)}
                r={2.2}
                fill={line.color}
                vectorEffect="non-scaling-stroke"
              >
                <title>{`${fmtDate(point.date)}: ${fmt(point.value)}`}</title>
              </circle>
            ))}
          </g>
        );
      })}
    </svg>
  );
}

export function VitalSignsChart({ patientId, clinicId }: { patientId: string; clinicId: string }) {
  const [notes, setNotes] = useState<ClinicalNoteResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [open, setOpen] = useState(true);

  useEffect(() => {
    setLoading(true);
    setError("");
    clinicalNotesApi
      .listByPatient(patientId, clinicId)
      .then(setNotes)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [patientId, clinicId]);

  const cards = useMemo<MetricCard[]>(() => {
    const dated = notes
      .filter((note) => note.vitalSigns)
      .map((note) => ({ date: note.createdAt, vitals: note.vitalSigns! }))
      .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());

    const pointsFor = (pick: (vitals: NonNullable<ClinicalNoteResponse["vitalSigns"]>) => number | null | undefined): Point[] =>
      dated
        .map(({ date, vitals }) => ({ date, value: pick(vitals) }))
        .filter((point): point is Point => typeof point.value === "number" && !Number.isNaN(point.value));

    const primary = "var(--color-primary)";
    const result: MetricCard[] = [];

    const weight = pointsFor((vitals) => vitals.weight);
    if (weight.length) result.push({ key: "weight", label: "Peso", unit: "kg", series: [{ color: primary, points: weight }] });

    const bmi = pointsFor((vitals) => vitals.bmi);
    if (bmi.length) result.push({ key: "bmi", label: "IMC", unit: "", series: [{ color: "var(--color-progress)", points: bmi }] });

    const systolic = pointsFor((vitals) => parseBloodPressure(vitals.bloodPressure)?.[0]);
    const diastolic = pointsFor((vitals) => parseBloodPressure(vitals.bloodPressure)?.[1]);
    if (systolic.length || diastolic.length) {
      result.push({
        key: "bp",
        label: "Presión arterial",
        unit: "mmHg",
        series: [
          { color: "var(--color-error)", legend: "Sistólica", points: systolic },
          { color: "var(--color-progress)", legend: "Diastólica", points: diastolic }
        ]
      });
    }

    const heartRate = pointsFor((vitals) => vitals.heartRate);
    if (heartRate.length)
      result.push({ key: "hr", label: "Frecuencia cardíaca", unit: "lpm", series: [{ color: "var(--color-error)", points: heartRate }] });

    const spo2 = pointsFor((vitals) => vitals.oxygenSaturation);
    if (spo2.length)
      result.push({ key: "spo2", label: "SpO₂", unit: "%", series: [{ color: "var(--color-progress)", points: spo2 }] });

    const temperature = pointsFor((vitals) => vitals.temperature);
    if (temperature.length)
      result.push({ key: "temp", label: "Temperatura", unit: "°C", series: [{ color: "var(--color-warning)", points: temperature }] });

    const respiratoryRate = pointsFor((vitals) => vitals.respiratoryRate);
    if (respiratoryRate.length)
      result.push({
        key: "rr",
        label: "Frecuencia respiratoria",
        unit: "rpm",
        series: [{ color: "var(--color-text-2)", points: respiratoryRate }]
      });

    return result;
  }, [notes]);

  if (loading) return null;

  const notesWithVitals = notes.filter((note) => note.vitalSigns).length;

  return (
    <section className="vitals-section">
      <button type="button" className="vitals-section-toggle" onClick={() => setOpen((value) => !value)} aria-expanded={open}>
        {open ? <IconChevronDown size={16} aria-hidden="true" /> : <IconChevronRight size={16} aria-hidden="true" />}
        <IconChartLine size={16} aria-hidden="true" />
        <span>Signos vitales</span>
        <span className="vitals-section-count">{notesWithVitals} registro(s)</span>
      </button>

      {open && (
        <div className="vitals-section-body">
          {error && <p className="alert error">{error}</p>}

          {!error && cards.length === 0 && (
            <p className="vitals-empty">Aún no hay signos vitales capturados en las notas clínicas de este paciente.</p>
          )}

          {cards.length > 0 && (
            <div className="vitals-grid">
              {cards.map((card) => {
                const last = card.series
                  .flatMap((line) => line.points)
                  .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())[0];
                const lastLabel =
                  card.key === "bp"
                    ? `${card.series[0].points.at(-1)?.value ?? "—"}/${card.series[1].points.at(-1)?.value ?? "—"}`
                    : last
                      ? fmt(last.value)
                      : "—";
                return (
                  <article key={card.key} className="vitals-card">
                    <header className="vitals-card-head">
                      <span className="vitals-card-label">{card.label}</span>
                      <span className="vitals-card-last">
                        {lastLabel}
                        {card.unit && <small> {card.unit}</small>}
                      </span>
                    </header>
                    <Sparkline series={card.series} />
                    <footer className="vitals-card-foot">
                      {card.series
                        .filter((line) => line.legend && line.points.length > 0)
                        .map((line) => (
                          <span key={line.legend} className="vitals-legend">
                            <span className="vitals-legend-dot" style={{ background: line.color }} />
                            {line.legend}
                          </span>
                        ))}
                      <span className="vitals-card-range">
                        {last ? `Último: ${fmtDate(last.date)}` : ""}
                      </span>
                    </footer>
                  </article>
                );
              })}
            </div>
          )}
        </div>
      )}
    </section>
  );
}
