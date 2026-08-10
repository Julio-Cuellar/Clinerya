import { FormEvent, useEffect, useMemo, useState } from "react";
import { IconBuildingHospital, IconInfoCircle, IconPlus, IconPower, IconUserMinus, IconUserPlus } from "@tabler/icons-react";
import type { ClinicRoomResponse } from "@modules/agenda/types";
import type { ClinicRoomStaffAssignmentResponse } from "@modules/clinics/types";
import { clinicRoomsApi, getFriendlyError, staffApi, type ClinicStaffResponse } from "@shared/api/api";

function formatDate(value?: string) {
  if (!value) return "Sin registro";
  return new Intl.DateTimeFormat("es-MX", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

export function ConsultoriosScreen({ clinicId, hasClinic }: { clinicId?: string; hasClinic: boolean }) {
  const [rooms, setRooms] = useState<ClinicRoomResponse[]>([]);
  const [doctors, setDoctors] = useState<ClinicStaffResponse[]>([]);
  const [assignments, setAssignments] = useState<ClinicRoomStaffAssignmentResponse[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState<string>();
  const [assignmentStaffId, setAssignmentStaffId] = useState("");
  const [loading, setLoading] = useState(true);
  const [assignmentLoading, setAssignmentLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [assignmentSaving, setAssignmentSaving] = useState(false);
  const [error, setError] = useState("");
  const [assignmentError, setAssignmentError] = useState("");
  const [success, setSuccess] = useState("");

  const selectedRoom = useMemo(
    () => rooms.find((room) => room.id === selectedRoomId) ?? rooms[0],
    [rooms, selectedRoomId]
  );

  const loadRooms = () => {
    if (!clinicId) return Promise.resolve();
    setLoading(true);
    return Promise.all([clinicRoomsApi.getRooms(clinicId), staffApi.list(clinicId, "DOCTOR")])
      .then(([data, doctorList]) => {
        setRooms(data);
        setDoctors(doctorList);
        setSelectedRoomId((current) => current && data.some((room) => room.id === current) ? current : data[0]?.id);
      })
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    void loadRooms();
  }, [clinicId]);

  const loadAssignments = () => {
    if (!clinicId || !selectedRoom?.id) {
      setAssignments([]);
      return Promise.resolve();
    }
    setAssignmentLoading(true);
    setAssignmentError("");
    return clinicRoomsApi
      .listAssignedStaff(clinicId, selectedRoom.id)
      .then(setAssignments)
      .catch((caught) => setAssignmentError(getFriendlyError(caught)))
      .finally(() => setAssignmentLoading(false));
  };

  useEffect(() => {
    void loadAssignments();
  }, [clinicId, selectedRoom?.id]);

  const assignedStaffIds = useMemo(() => new Set(assignments.map((assignment) => assignment.staffId)), [assignments]);
  const availableDoctors = useMemo(
    () => doctors.filter((doctor) => !assignedStaffIds.has(doctor.staffId)),
    [doctors, assignedStaffIds]
  );

  const assignDoctor = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!clinicId || !selectedRoom?.id || !assignmentStaffId) return;

    setAssignmentSaving(true);
    setAssignmentError("");
    try {
      await clinicRoomsApi.assignStaff(clinicId, selectedRoom.id, assignmentStaffId);
      setAssignmentStaffId("");
      await loadAssignments();
    } catch (caught) {
      setAssignmentError(getFriendlyError(caught));
    } finally {
      setAssignmentSaving(false);
    }
  };

  const unassignDoctor = async (staffId: string) => {
    if (!clinicId || !selectedRoom?.id) return;
    setAssignmentSaving(true);
    setAssignmentError("");
    try {
      await clinicRoomsApi.unassignStaff(clinicId, selectedRoom.id, staffId);
      await loadAssignments();
    } catch (caught) {
      setAssignmentError(getFriendlyError(caught));
    } finally {
      setAssignmentSaving(false);
    }
  };

  const createRoom = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!clinicId) return;

    setSaving(true);
    setError("");
    setSuccess("");
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const value = (name: string) => String(form.get(name) ?? "").trim();

    try {
      const created = await clinicRoomsApi.createRoom(clinicId, {
        name: value("name"),
        code: value("code") || undefined,
        description: value("description") || undefined,
        colorHex: value("colorHex") || undefined
      });
      formElement.reset();
      setSuccess("Consultorio creado y asignado a la clínica activa.");
      setSelectedRoomId(created.id);
      await loadRooms();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  const toggleRoom = async () => {
    if (!clinicId || !selectedRoom) return;
    setError("");
    setSuccess("");
    try {
      if (selectedRoom.active) {
        await clinicRoomsApi.deactivateRoom(clinicId, selectedRoom.id);
      } else {
        await clinicRoomsApi.activateRoom(clinicId, selectedRoom.id);
      }
      setSuccess(selectedRoom.active ? "Consultorio desactivado." : "Consultorio activado.");
      await loadRooms();
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  if (!hasClinic || !clinicId) {
    return <section className="panel full"><p className="alert error">No hay una clínica activa para administrar consultorios.</p></section>;
  }

  return (
    <section className="rooms-page">
      <div className="rooms-page-intro">
        <div>
          <h2>Consultorios</h2>
          <p>Administra los espacios de atención de la clínica y consulta su estado operativo.</p>
        </div>
        <span className="badge neutral">{rooms.length} registrados</span>
      </div>

      <div className="rooms-grid">
        <section className="panel rooms-list-panel">
          <div className="panel-heading">
            <div>
              <h3>Consultorios de la clínica</h3>
              <p className="description">Selecciona uno para ver su resumen.</p>
            </div>
            <IconBuildingHospital size={22} aria-hidden="true" />
          </div>

          <form className="profile-form" onSubmit={createRoom}>
            <label className="field"><span>Nombre</span><input name="name" required placeholder="Ej. Consultorio 1" /></label>
            <label className="field"><span>Código interno</span><input name="code" placeholder="Ej. CONS-01" /></label>
            <label className="field"><span>Descripción</span><input name="description" placeholder="Ej. Odontología general" /></label>
            <label className="field"><span>Color en agenda</span><input name="colorHex" type="color" defaultValue="#3B82F6" style={{ height: "40px", cursor: "pointer" }} /></label>
            {error && <p className="alert error">{error}</p>}
            {success && <p className="alert success">{success}</p>}
            <button className="btn primary" type="submit" disabled={saving}>
              <IconPlus size={18} aria-hidden="true" />
              {saving ? "Creando..." : "Crear consultorio"}
            </button>
          </form>

          <div className="rooms-list" aria-live="polite">
            {loading && <p className="description">Cargando consultorios...</p>}
            {!loading && rooms.length === 0 && <p className="description">Todavía no hay consultorios registrados.</p>}
            {rooms.map((room) => (
              <button
                className={`room-list-item ${selectedRoom?.id === room.id ? "active" : ""}`}
                type="button"
                key={room.id}
                onClick={() => setSelectedRoomId(room.id)}
              >
                <span className="room-color-swatch" style={{ background: room.colorHex || "#3B82F6" }} />
                <span className="room-list-copy">
                  <strong>{room.name}</strong>
                  <small>{room.code || "Sin código"}</small>
                </span>
                <span className={`badge ${room.active ? "success" : "neutral"}`}>{room.active ? "Activo" : "Inactivo"}</span>
              </button>
            ))}
          </div>
        </section>

        <section className="panel room-summary-panel">
          <div className="panel-heading">
            <div>
              <h3>Resumen del consultorio</h3>
              <p className="description">Propiedades y estado del espacio seleccionado.</p>
            </div>
            <IconInfoCircle size={22} aria-hidden="true" />
          </div>

          {selectedRoom ? (
            <>
              <div className="room-summary-hero">
                <span className="room-color-swatch large" style={{ background: selectedRoom.colorHex || "#3B82F6" }} />
                <div>
                  <h2>{selectedRoom.name}</h2>
                  <p>{selectedRoom.description || "Sin descripción registrada"}</p>
                </div>
                <span className={`badge ${selectedRoom.active ? "success" : "neutral"}`}>{selectedRoom.active ? "Activo" : "Inactivo"}</span>
              </div>

              <div className="room-summary-grid">
                <div><label>Código interno</label><strong>{selectedRoom.code || "Sin código"}</strong></div>
                <div><label>Asignación</label><strong>Clínica activa</strong></div>
                <div><label>Creado</label><strong>{formatDate(selectedRoom.createdAt)}</strong></div>
                <div><label>Actualizado</label><strong>{formatDate(selectedRoom.updatedAt)}</strong></div>
              </div>

              <div className="room-summary-note">
                <IconInfoCircle size={17} aria-hidden="true" />
                <span>Este consultorio aparecerá como opción al crear o editar citas en Agenda.</span>
              </div>

              <div className="room-assignment-section">
                <div className="panel-heading">
                  <div>
                    <h4>Medicos asignados</h4>
                    <p className="description">Define que doctores pueden atender en este consultorio.</p>
                  </div>
                  <span className="badge neutral">{assignments.length}</span>
                </div>

                <form className="room-assignment-form" onSubmit={assignDoctor}>
                  <select
                    value={assignmentStaffId}
                    onChange={(event) => setAssignmentStaffId(event.target.value)}
                    disabled={assignmentSaving || availableDoctors.length === 0}
                  >
                    <option value="">Selecciona un medico</option>
                    {availableDoctors.map((doctor) => (
                      <option value={doctor.staffId} key={doctor.staffId}>{doctor.fullName}</option>
                    ))}
                  </select>
                  <button className="btn secondary" type="submit" disabled={assignmentSaving || !assignmentStaffId}>
                    <IconUserPlus size={17} aria-hidden="true" />
                    Asignar medico
                  </button>
                </form>

                {assignmentError && <p className="alert error">{assignmentError}</p>}
                {assignmentLoading && <p className="description">Cargando asignaciones...</p>}
                {!assignmentLoading && assignments.length === 0 && (
                  <p className="description">Todavia no hay medicos asignados a este consultorio.</p>
                )}
                <div className="room-assignment-list">
                  {assignments.map((assignment) => {
                    const doctor = doctors.find((member) => member.staffId === assignment.staffId);
                    return (
                      <div className="room-assignment-row" key={assignment.id}>
                        <div>
                          <strong>{doctor?.fullName || "Medico no encontrado"}</strong>
                          <span>Doctor / Especialista</span>
                        </div>
                        <button
                          className="icon-btn"
                          type="button"
                          aria-label={`Retirar a ${doctor?.fullName || "medico"}`}
                          title="Retirar medico"
                          onClick={() => void unassignDoctor(assignment.staffId)}
                          disabled={assignmentSaving}
                        >
                          <IconUserMinus size={17} aria-hidden="true" />
                        </button>
                      </div>
                    );
                  })}
                </div>
              </div>

              <button className={`btn ${selectedRoom.active ? "secondary" : "primary"}`} type="button" onClick={toggleRoom}>
                <IconPower size={17} aria-hidden="true" />
                {selectedRoom.active ? "Desactivar consultorio" : "Activar consultorio"}
              </button>
            </>
          ) : (
            <div className="room-empty-state">
              <IconBuildingHospital size={34} aria-hidden="true" />
              <strong>Selecciona un consultorio</strong>
              <span>El resumen del espacio aparecerá aquí.</span>
            </div>
          )}
        </section>
      </div>
    </section>
  );
}
