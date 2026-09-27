package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort.UpcomingVisit;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;
import com.jclinical.automation.domain.service.SlotLabel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Proximas citas de quien escribe (de todos los pacientes registrados con ese celular). */
public final class MyAppointmentsTool implements AgentTool {

    public static final String NAME = "mis_citas";
    private static final int PER_PATIENT = 5;

    private final PatientAppointmentsPort appointments;
    private final DoctorDirectoryPort doctors;

    public MyAppointmentsTool(PatientAppointmentsPort appointments, DoctorDirectoryPort doctors) {
        this.appointments = appointments;
        this.doctors = doctors;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Próximas citas del paciente que escribe: fecha, hora, médico y si ya está confirmada.", List.of());
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        if (context.patients().isEmpty()) {
            return ToolOutcome.of(Map.of("paciente_registrado", false));
        }
        Map<UUID, String> doctorNames = doctors.listDoctors(context.clinicId()).stream()
                .collect(Collectors.toMap(DoctorContact::staffId, DoctorContact::displayName, (first, second) -> first));
        List<Map<String, Object>> visits = new ArrayList<>();
        List<String> facts = new ArrayList<>();
        for (PatientContact patient : context.patients()) {
            for (UpcomingVisit visit : appointments.upcoming(context.clinicId(), patient.patientId(), PER_PATIENT)) {
                String date = SlotLabel.of(visit.start());
                String doctor = doctorNames.getOrDefault(visit.doctorStaffId(), "tu médico");
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("paciente", patient.displayName());
                row.put("fecha", date);
                row.put("medico", doctor);
                row.put("confirmada", visit.confirmed());
                visits.add(Map.copyOf(row));
                facts.add(date);
                facts.add(doctor);
            }
        }
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("paciente_registrado", true);
        content.put("citas", List.copyOf(visits));
        return new ToolOutcome(content, List.of(), facts);
    }
}
