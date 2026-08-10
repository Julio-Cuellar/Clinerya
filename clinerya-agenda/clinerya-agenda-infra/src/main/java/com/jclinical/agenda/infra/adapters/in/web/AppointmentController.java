package com.jclinical.agenda.infra.adapters.in.web;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase.CreateAppointmentCommand;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase.CreateAppointmentSeriesCommand;
import com.jclinical.agenda.infra.adapters.in.web.dto.AppointmentResponse;
import com.jclinical.agenda.infra.adapters.in.web.dto.AssignPatientRequest;
import com.jclinical.agenda.infra.adapters.in.web.dto.CreateAppointmentRequest;
import com.jclinical.agenda.infra.adapters.in.web.dto.CreateAppointmentSeriesRequest;
import com.jclinical.agenda.infra.adapters.in.web.dto.DoctorResponse;
import com.jclinical.agenda.infra.adapters.in.web.dto.RescheduleAppointmentRequest;
import com.jclinical.agenda.infra.adapters.in.web.dto.TransitionAppointmentStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}")
@RequiredArgsConstructor
public class AppointmentController {

    private final ManageAppointmentsUseCase appointmentsUseCase;

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @PathVariable UUID clinicId,
            @RequestBody CreateAppointmentRequest request) {
        CreateAppointmentCommand command = new CreateAppointmentCommand(
                request.patientId(),
                request.doctorStaffId(),
                request.roomId(),
                request.quotationId(),
                request.quotationItemId(),
                request.quotationItemIds(),
                request.scheduledStart(),
                request.scheduledEnd(),
                request.reason(),
                request.notes()
        );
        Appointment appointment = appointmentsUseCase.createAppointment(clinicId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(appointment));
    }

    @PostMapping("/appointments/series")
    public ResponseEntity<List<AppointmentResponse>> createAppointmentSeries(
            @PathVariable UUID clinicId,
            @RequestBody CreateAppointmentSeriesRequest request) {
        CreateAppointmentSeriesCommand command = new CreateAppointmentSeriesCommand(
                request.patientId(),
                request.doctorStaffId(),
                request.roomId(),
                request.quotationId(),
                request.quotationItemId(),
                request.quotationItemIds(),
                request.firstScheduledStart(),
                request.firstScheduledEnd(),
                request.frequency(),
                request.repeatCount(),
                request.reason(),
                request.notes()
        );
        List<Appointment> series = appointmentsUseCase.createAppointmentSeries(clinicId, command);
        List<AppointmentResponse> responses = series.stream().map(this::toResponse).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponse>> listAppointments(
            @PathVariable UUID clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<AppointmentResponse> responses = appointmentsUseCase.listByClinicRange(clinicId, from, to).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointment(
            @PathVariable UUID clinicId,
            @PathVariable UUID appointmentId) {
        Appointment appointment = appointmentsUseCase.getAppointment(appointmentId, clinicId);
        return ResponseEntity.ok(toResponse(appointment));
    }

    @GetMapping("/appointments/by-quotation/{quotationId}")
    public ResponseEntity<List<AppointmentResponse>> listByQuotation(
            @PathVariable UUID clinicId,
            @PathVariable UUID quotationId) {
        List<AppointmentResponse> responses = appointmentsUseCase.listByQuotation(quotationId, clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/appointments/{appointmentId}/reschedule")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable UUID clinicId,
            @PathVariable UUID appointmentId,
            @RequestBody RescheduleAppointmentRequest request) {
        Appointment appointment = appointmentsUseCase.rescheduleAppointment(
                appointmentId, clinicId, request.scheduledStart(), request.scheduledEnd());
        return ResponseEntity.ok(toResponse(appointment));
    }

    @PatchMapping("/appointments/{appointmentId}/status")
    public ResponseEntity<AppointmentResponse> transitionStatus(
            @PathVariable UUID clinicId,
            @PathVariable UUID appointmentId,
            @RequestBody TransitionAppointmentStatusRequest request) {
        Appointment appointment = appointmentsUseCase.transitionStatus(
                appointmentId, clinicId, request.status(), request.cancellationReason(), null);
        return ResponseEntity.ok(toResponse(appointment));
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorResponse>> listDoctors(@PathVariable UUID clinicId) {
        List<DoctorResponse> responses = appointmentsUseCase.listDoctors(clinicId).stream()
                .map(doctor -> new DoctorResponse(doctor.staffId(), doctor.fullName()))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/appointments/without-patient")
    public ResponseEntity<List<AppointmentResponse>> listWithoutPatient(@PathVariable UUID clinicId) {
        List<AppointmentResponse> responses = appointmentsUseCase.listWithoutPatient(clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/appointments/{appointmentId}")
    public ResponseEntity<Void> deleteAppointment(
            @PathVariable UUID clinicId,
            @PathVariable UUID appointmentId) {
        appointmentsUseCase.deleteAppointment(appointmentId, clinicId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/appointments/{appointmentId}/assign-patient")
    public ResponseEntity<AppointmentResponse> assignPatient(
            @PathVariable UUID clinicId,
            @PathVariable UUID appointmentId,
            @RequestBody AssignPatientRequest request) {
        Appointment appointment = appointmentsUseCase.assignPatient(appointmentId, clinicId, request.patientId());
        return ResponseEntity.ok(toResponse(appointment));
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getClinicId(),
                appointment.getPatientId(),
                appointment.getDoctorStaffId(),
                appointment.getRoomId(),
                appointment.getQuotationId(),
                appointment.getQuotationItemId(),
                appointment.getQuotationItemIds(),
                appointment.getSeriesId(),
                appointment.getScheduledStart(),
                appointment.getScheduledEnd(),
                appointment.getReason(),
                appointment.getNotes(),
                appointment.getCancellationReason(),
                appointment.getCancelledAt(),
                appointment.getCancelledByUserId(),
                appointment.getStatus(),
                appointment.isMaterialsReserved(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}
