import { useMemo, useState } from "react";
import { IconSearch } from "@tabler/icons-react";
import { QuotationsPanel } from "@modules/treatments/components/QuotationsPanel";
import { TreatmentCatalogPanel } from "@modules/treatments/components/TreatmentCatalogPanel";
import type { ClinicResponse } from "@modules/clinics/types";
import type { PatientResponse } from "@modules/patients/types";
import { getClinicProfileFor } from "@shared/utils/clinicProfile";

type Tab = "catalogo" | "cotizaciones";

export function TratamientosScreen({
  clinicId,
  clinic,
  hasClinic,
  patients,
  canSeeQuotations
}: {
  clinicId?: string;
  clinic?: ClinicResponse;
  hasClinic: boolean;
  patients: PatientResponse[];
  /**
   * Los presupuestos son parte del expediente (el backend exige VIEW_MEDICAL_RECORDS). Quien no
   * atiende pacientes, como un administrador que solo gestiona, trabaja únicamente el catálogo.
   */
  canSeeQuotations: boolean;
}) {
  const profile = getClinicProfileFor(clinic);
  const [tab, setTab] = useState<Tab>("catalogo");
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [search, setSearch] = useState("");

  const filteredPatients = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return patients;
    return patients.filter((patient) =>
      [patient.firstName, patient.lastNamePaterno, patient.lastNameMaterno, patient.curp]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
        .includes(term)
    );
  }, [patients, search]);

  return (
    <section className="dashboard-grid">
      {canSeeQuotations && (
        <div className="tab-switch">
          <button className={tab === "catalogo" ? "active" : ""} type="button" onClick={() => setTab("catalogo")}>
            Catálogo
          </button>
          <button className={tab === "cotizaciones" ? "active" : ""} type="button" onClick={() => setTab("cotizaciones")}>
            Cotizaciones
          </button>
        </div>
      )}

      {tab === "catalogo" && <TreatmentCatalogPanel clinicId={clinicId} hasClinic={hasClinic} profile={profile} />}

      {tab === "cotizaciones" && canSeeQuotations &&
        (selectedPatient && clinicId ? (
          <QuotationsPanel
            clinicId={clinicId}
            clinic={clinic}
            patient={selectedPatient}
            onChangePatient={() => setSelectedPatient(null)}
          />
        ) : (
          <article className="panel full">
            <div className="panel-heading">
              <h2>Selecciona un paciente</h2>
              <span className="badge neutral">{patients.length}</span>
            </div>

            {!hasClinic && (
              <div className="clinic-list">
                <div className="clinic-row">
                  <strong>Completa los datos de tu clínica</strong>
                  <span>Necesitas una clínica activa para ver cotizaciones</span>
                </div>
              </div>
            )}

            {hasClinic && patients.length > 0 && (
              <div className="table-toolbar">
                <label className="search-field">
                  <IconSearch size={16} aria-hidden="true" />
                  <input
                    type="search"
                    placeholder="Buscar paciente por nombre o CURP"
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                  />
                </label>
              </div>
            )}

            {hasClinic && patients.length === 0 && (
              <div className="clinic-list">
                <div className="clinic-row">
                  <strong>Sin pacientes registrados</strong>
                  <span>Registra pacientes en el módulo Pacientes para poder cotizar tratamientos</span>
                </div>
              </div>
            )}

            {hasClinic && patients.length > 0 && (
              <div className="table-wrapper">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Paciente</th>
                      <th>CURP</th>
                      <th>Contacto</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredPatients.map((patient) => (
                      <tr key={patient.id} onClick={() => setSelectedPatient(patient)}>
                        <td>
                          <strong>
                            {patient.firstName} {patient.lastNamePaterno} {patient.lastNameMaterno}
                          </strong>
                        </td>
                        <td>{patient.curp || "Sin registrar"}</td>
                        <td>{patient.phone || patient.email || "Sin contacto"}</td>
                      </tr>
                    ))}
                    {filteredPatients.length === 0 && (
                      <tr>
                        <td colSpan={3}>Sin resultados para "{search}"</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </article>
        ))}
    </section>
  );
}
