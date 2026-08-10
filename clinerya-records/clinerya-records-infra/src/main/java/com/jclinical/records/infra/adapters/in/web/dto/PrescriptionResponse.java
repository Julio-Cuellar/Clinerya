package com.jclinical.records.infra.adapters.in.web.dto;

import com.jclinical.records.domain.model.Prescription;
import com.jclinical.records.domain.model.PrescriptionItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponse {
    private UUID id;
    private UUID clinicId;
    private UUID patientId;
    private UUID doctorId;
    private UUID appointmentId;
    private String notes;
    private String status;
    private List<ItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDto {
        private UUID id;
        private String medicationName;
        private String dosage;
        private String frequency;
        private String duration;
        private String instructions;
    }

    public static PrescriptionResponse fromDomain(Prescription domain) {
        if (domain == null) return null;
        List<ItemDto> itemDtos = domain.getItems() == null ? List.of() :
                domain.getItems().stream()
                        .map(item -> ItemDto.builder()
                                .id(item.getId())
                                .medicationName(item.getMedicationName())
                                .dosage(item.getDosage())
                                .frequency(item.getFrequency())
                                .duration(item.getDuration())
                                .instructions(item.getInstructions())
                                .build())
                        .collect(Collectors.toList());

        return PrescriptionResponse.builder()
                .id(domain.getId())
                .clinicId(domain.getClinicId())
                .patientId(domain.getPatientId())
                .doctorId(domain.getDoctorId())
                .appointmentId(domain.getAppointmentId())
                .notes(domain.getNotes())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .items(itemDtos)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
