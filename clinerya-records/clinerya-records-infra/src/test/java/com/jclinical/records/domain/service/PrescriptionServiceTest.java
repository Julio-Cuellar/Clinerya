package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.Prescription;
import com.jclinical.records.domain.model.PrescriptionItem;
import com.jclinical.records.domain.model.PrescriptionStatus;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;
import com.jclinical.records.domain.ports.out.PrescriptionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepositoryPort prescriptionRepository;
    @Mock
    private PatientValidatorPort patientValidator;
    @Mock
    private PatientAccessAuthorizationPort accessAuthorization;

    private PrescriptionService service;

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PrescriptionService(prescriptionRepository, patientValidator, accessAuthorization);
    }

    private void grant(AccessLevel level) {
        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(accessAuthorization.resolveAccess(userId, clinicId, patientId))
                .thenReturn(new AccessDecision(level, false));
    }

    private List<PrescriptionItem> oneItem() {
        return List.of(PrescriptionItem.builder().medicationName("Amoxicilina").dosage("500 mg").build());
    }

    @Test
    void issueDeniedWhenNoAccess() {
        grant(AccessLevel.NONE);

        assertThatThrownBy(() -> service.issuePrescription(
                clinicId, patientId, doctorId, null, "notas", oneItem(), userId))
                .isInstanceOf(ClinicAccessDeniedException.class);

        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void issueDeniedWhenOnlyReadAccess() {
        grant(AccessLevel.READ_ONLY);

        assertThatThrownBy(() -> service.issuePrescription(
                clinicId, patientId, doctorId, null, "notas", oneItem(), userId))
                .isInstanceOf(ClinicAccessDeniedException.class);

        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void issueFailsWhenPatientNotInClinic() {
        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(false);

        assertThatThrownBy(() -> service.issuePrescription(
                clinicId, patientId, doctorId, null, "notas", oneItem(), userId))
                .isInstanceOf(IllegalArgumentException.class);

        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void issueSucceedsWithWriteAccess() {
        grant(AccessLevel.READ_WRITE);
        when(prescriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Prescription issued = service.issuePrescription(
                clinicId, patientId, doctorId, null, "notas", oneItem(), userId);

        assertThat(issued.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
        assertThat(issued.getItems()).hasSize(1);
        assertThat(issued.getItems().get(0).getId()).isNotNull();
        assertThat(issued.getItems().get(0).getPrescriptionId()).isEqualTo(issued.getId());
        verify(prescriptionRepository).save(any());
    }

    @Test
    void listDeniedWhenNoAccess() {
        grant(AccessLevel.NONE);

        assertThatThrownBy(() -> service.getPrescriptionsByPatient(clinicId, patientId, userId))
                .isInstanceOf(ClinicAccessDeniedException.class);

        verify(prescriptionRepository, never()).findByClinicIdAndPatientId(any(), any());
    }

    @Test
    void listAllowedWithReadOnlyAccess() {
        grant(AccessLevel.READ_ONLY);
        Prescription p = Prescription.builder().id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId).build();
        when(prescriptionRepository.findByClinicIdAndPatientId(clinicId, patientId)).thenReturn(List.of(p));

        assertThat(service.getPrescriptionsByPatient(clinicId, patientId, userId)).containsExactly(p);
    }

    @Test
    void getByIdAuthorizesAgainstThePrescriptionPatient() {
        UUID prescriptionId = UUID.randomUUID();
        Prescription stored = Prescription.builder()
                .id(prescriptionId).clinicId(clinicId).patientId(patientId).build();
        when(prescriptionRepository.findById(clinicId, prescriptionId)).thenReturn(Optional.of(stored));
        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(accessAuthorization.resolveAccess(userId, clinicId, patientId))
                .thenReturn(new AccessDecision(AccessLevel.NONE, false));

        assertThatThrownBy(() -> service.getPrescriptionById(clinicId, prescriptionId, userId))
                .isInstanceOf(ClinicAccessDeniedException.class);

        verify(accessAuthorization).resolveAccess(userId, clinicId, patientId);
    }

    @Test
    void getByIdFailsWhenNotFound() {
        UUID prescriptionId = UUID.randomUUID();
        when(prescriptionRepository.findById(clinicId, prescriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPrescriptionById(clinicId, prescriptionId, userId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
