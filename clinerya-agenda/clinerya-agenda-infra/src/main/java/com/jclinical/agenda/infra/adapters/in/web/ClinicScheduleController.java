package com.jclinical.agenda.infra.adapters.in.web;

import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.ports.in.ManageClinicScheduleUseCase;
import com.jclinical.agenda.domain.ports.in.ManageClinicScheduleUseCase.DayScheduleCommand;
import com.jclinical.agenda.infra.adapters.in.web.dto.DayScheduleRequest;
import com.jclinical.agenda.infra.adapters.in.web.dto.DayScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/schedule")
@RequiredArgsConstructor
public class ClinicScheduleController {

    private final ManageClinicScheduleUseCase clinicScheduleUseCase;

    @GetMapping
    public ResponseEntity<List<DayScheduleResponse>> getSchedule(@PathVariable UUID clinicId) {
        List<DayScheduleResponse> responses = clinicScheduleUseCase.getSchedule(clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping
    public ResponseEntity<List<DayScheduleResponse>> updateSchedule(
            @PathVariable UUID clinicId,
            @RequestBody List<DayScheduleRequest> requests) {
        List<DayScheduleCommand> commands = requests.stream()
                .map(request -> new DayScheduleCommand(request.dayOfWeek(), request.open(), request.startTime(), request.endTime()))
                .toList();
        List<DayScheduleResponse> responses = clinicScheduleUseCase.updateSchedule(clinicId, commands).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    private DayScheduleResponse toResponse(ClinicSchedule schedule) {
        return new DayScheduleResponse(schedule.getDayOfWeek(), schedule.isOpen(), schedule.getStartTime(), schedule.getEndTime());
    }
}
