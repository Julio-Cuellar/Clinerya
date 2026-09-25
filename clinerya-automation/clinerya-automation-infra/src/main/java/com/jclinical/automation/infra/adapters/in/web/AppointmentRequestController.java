package com.jclinical.automation.infra.adapters.in.web;

import com.jclinical.automation.domain.model.AppointmentRequest;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.ports.in.RespondAppointmentRequestUseCase;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Bandeja del medico: sus solicitudes de cita pendientes y sus tres respuestas. El servicio verifica
 * que quien responde sea el medico de la solicitud.
 */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/appointment-requests")
@RequiredArgsConstructor
public class AppointmentRequestController {

    private final RespondAppointmentRequestUseCase requests;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public List<AppointmentRequestResponse> listPending(@PathVariable UUID clinicId) {
        return requests.listPending(currentUserResolver.getCurrentUserId(), clinicId).stream()
                .map(AppointmentRequestResponse::from)
                .toList();
    }

    @GetMapping("/{requestId}/proposable-slots")
    public List<SlotPayload> proposableSlots(@PathVariable UUID clinicId, @PathVariable UUID requestId) {
        return requests.proposableSlots(currentUserResolver.getCurrentUserId(), clinicId, requestId).stream()
                .map(slot -> new SlotPayload(slot.start(), slot.end()))
                .toList();
    }

    @PostMapping("/{requestId}/accept")
    public AppointmentRequestResponse accept(@PathVariable UUID clinicId, @PathVariable UUID requestId) {
        return AppointmentRequestResponse.from(requests.accept(currentUserResolver.getCurrentUserId(), clinicId, requestId));
    }

    @PostMapping("/{requestId}/reject")
    public AppointmentRequestResponse reject(@PathVariable UUID clinicId, @PathVariable UUID requestId,
                                             @RequestBody(required = false) RejectRequest body) {
        String reason = body == null ? null : body.reason();
        return AppointmentRequestResponse.from(
                requests.reject(currentUserResolver.getCurrentUserId(), clinicId, requestId, reason));
    }

    @PostMapping("/{requestId}/propose")
    public AppointmentRequestResponse propose(@PathVariable UUID clinicId, @PathVariable UUID requestId,
                                              @RequestBody ProposeRequest body) {
        List<AvailableSlot> options = body == null || body.options() == null ? List.of() : body.options().stream()
                .map(AppointmentRequestController::toSlot)
                .toList();
        return AppointmentRequestResponse.from(
                requests.proposeOptions(currentUserResolver.getCurrentUserId(), clinicId, requestId, options));
    }

    private static AvailableSlot toSlot(SlotPayload slot) {
        if (slot == null || slot.start() == null || slot.end() == null) {
            throw new IllegalArgumentException("Cada horario propuesto necesita inicio y fin.");
        }
        return new AvailableSlot(slot.start(), slot.end());
    }

    public record SlotPayload(LocalDateTime start, LocalDateTime end) {}

    public record RejectRequest(String reason) {}

    public record ProposeRequest(List<SlotPayload> options) {}

    public record AppointmentRequestResponse(
            UUID id,
            UUID patientId,
            String patientName,
            UUID doctorStaffId,
            String doctorName,
            LocalDateTime start,
            LocalDateTime end,
            String status,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            UUID appointmentId,
            String rejectionReason,
            List<SlotPayload> proposedOptions
    ) {
        static AppointmentRequestResponse from(AppointmentRequest request) {
            return new AppointmentRequestResponse(request.id(), request.patientId(), request.patientName(),
                    request.doctorStaffId(), request.doctorName(), request.start(), request.end(),
                    request.status().name(), request.createdAt(),
                    request.createdAt().plus(AppointmentRequest.RESPONSE_WINDOW), request.appointmentId(),
                    request.rejectionReason(), request.proposedOptions().stream()
                            .map(option -> new SlotPayload(option.start(), option.end()))
                            .toList());
        }
    }
}
