package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.AllergyCategory;
import com.jclinical.records.domain.model.AllergySeverity;
import com.jclinical.records.domain.model.ClinicalDataSource;
import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.ConditionStatus;
import com.jclinical.records.domain.model.ClinicalReviewKind;
import com.jclinical.records.domain.model.PatientAllergy;
import com.jclinical.records.domain.model.PatientClinicalReview;
import com.jclinical.records.domain.model.PatientCondition;
import com.jclinical.records.domain.model.PatientMedication;
import com.jclinical.records.domain.ports.in.ManagePatientClinicalSummaryUseCase.AllergyInput;
import com.jclinical.records.domain.ports.in.ManagePatientClinicalSummaryUseCase.ClinicalSummary;
import com.jclinical.records.domain.ports.in.ManagePatientClinicalSummaryUseCase.ConditionInput;
import com.jclinical.records.domain.ports.in.ManagePatientClinicalSummaryUseCase.MedicationInput;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.ports.out.PatientAllergyRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientClinicalReviewRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientConditionRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientLookupPort;
import com.jclinical.records.domain.ports.out.PatientLookupPort.PatientDetails;
import com.jclinical.records.domain.ports.out.PatientMedicationRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientClinicalSummaryServiceTest {

    @Mock private PatientAllergyRepositoryPort allergyRepository;
    @Mock private PatientConditionRepositoryPort conditionRepository;
    @Mock private PatientMedicationRepositoryPort medicationRepository;
    @Mock private PatientClinicalReviewRepositoryPort clinicalReviewRepository;
    @Mock private PatientValidatorPort patientValidator;
    @Mock private PatientAccessAuthorizationPort accessAuthorization;
    @Mock private PatientLookupPort patientLookup;
    @Mock private ClinicalNoteRepositoryPort noteRepository;

    private PatientClinicalSummaryService service;

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PatientClinicalSummaryService(allergyRepository, conditionRepository, medicationRepository,
                clinicalReviewRepository, patientValidator, accessAuthorization, patientLookup, noteRepository);
    }

    private void grant(AccessLevel level) {
        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(accessAuthorization.resolveAccess(userId, clinicId, patientId))
                .thenReturn(new AccessDecision(level, false));
    }

    @Test
    void getSummaryDeniedWithoutAccess() {
        grant(AccessLevel.NONE);
        assertThatThrownBy(() -> service.getSummary(patientId, clinicId, userId))
                .isInstanceOf(ClinicAccessDeniedException.class);
    }

    @Test
    void getSummaryAssemblesBloodTypeAndFiltersActiveMedications() {
        grant(AccessLevel.READ_ONLY);
        when(patientLookup.findPatient(patientId)).thenReturn(Optional.of(
                new PatientDetails(patientId, clinicId, "Ana Ruiz", null, null, null, "O_POSITIVE")));
        when(allergyRepository.findByClinicIdAndPatientId(clinicId, patientId))
                .thenReturn(List.of(PatientAllergy.builder().id(UUID.randomUUID()).substance("Penicilina").build()));
        when(conditionRepository.findByClinicIdAndPatientId(clinicId, patientId))
                .thenReturn(List.of(PatientCondition.builder().id(UUID.randomUUID()).name("Diabetes").build()));
        when(medicationRepository.findByClinicIdAndPatientId(clinicId, patientId)).thenReturn(List.of(
                PatientMedication.builder().id(UUID.randomUUID()).medicationName("Metformina").active(true).build(),
                PatientMedication.builder().id(UUID.randomUUID()).medicationName("Amoxicilina").active(false).build()));
        LocalDateTime noteAt = LocalDateTime.now().minusDays(2);
        when(noteRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId))
                .thenReturn(List.of(ClinicalNote.builder().id(UUID.randomUUID()).createdAt(noteAt).build()));

        ClinicalSummary summary = service.getSummary(patientId, clinicId, userId);

        assertThat(summary.bloodType()).isEqualTo("O_POSITIVE");
        assertThat(summary.allergies()).hasSize(1);
        assertThat(summary.conditions()).hasSize(1);
        assertThat(summary.activeMedications()).extracting(PatientMedication::getMedicationName)
                .containsExactly("Metformina");
        assertThat(summary.lastNoteAt()).isEqualTo(noteAt);
    }

    @Test
    void addAllergyDeniedWhenOnlyReadAccess() {
        grant(AccessLevel.READ_ONLY);
        assertThatThrownBy(() -> service.addAllergy(patientId, clinicId, userId, "Dra. Ruiz",
                new AllergyInput("Penicilina", "Urticaria", null, null)))
                .isInstanceOf(ClinicAccessDeniedException.class);
        verify(allergyRepository, never()).save(any());
    }

    @Test
    void addAllergyPersistsWithMetadataAndDefaults() {
        grant(AccessLevel.READ_WRITE);
        when(allergyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addAllergy(patientId, clinicId, userId, "Dra. Ruiz",
                new AllergyInput("  Penicilina  ", "Urticaria", null, null));

        ArgumentCaptor<PatientAllergy> captor = ArgumentCaptor.forClass(PatientAllergy.class);
        verify(allergyRepository).save(captor.capture());
        PatientAllergy saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSubstance()).isEqualTo("Penicilina");
        assertThat(saved.getSeverity()).isEqualTo(AllergySeverity.UNKNOWN);
        assertThat(saved.getCategory()).isEqualTo(AllergyCategory.DRUG);
        assertThat(saved.getSource()).isEqualTo(ClinicalDataSource.MANUAL);
        assertThat(saved.getNotedByUserId()).isEqualTo(userId);
        assertThat(saved.getNotedByUserName()).isEqualTo("Dra. Ruiz");
        assertThat(saved.getNotedAt()).isNotNull();
    }

    @Test
    void updateConditionRejectsRowFromAnotherPatient() {
        grant(AccessLevel.READ_WRITE);
        UUID conditionId = UUID.randomUUID();
        when(conditionRepository.findByIdAndClinicId(conditionId, clinicId)).thenReturn(Optional.of(
                PatientCondition.builder().id(conditionId).clinicId(clinicId).patientId(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> service.updateCondition(conditionId, patientId, clinicId, userId, "Dra. Ruiz",
                new ConditionInput("Hipertension", "I10", ConditionStatus.ACTIVE, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(conditionRepository, never()).save(any());
    }

    @Test
    void removeMedicationDeletesWhenOwnedByPatient() {
        grant(AccessLevel.READ_WRITE);
        UUID medicationId = UUID.randomUUID();
        when(medicationRepository.findByIdAndClinicId(medicationId, clinicId)).thenReturn(Optional.of(
                PatientMedication.builder().id(medicationId).clinicId(clinicId).patientId(patientId).build()));

        service.removeMedication(medicationId, patientId, clinicId, userId);

        verify(medicationRepository).deleteByIdAndClinicId(medicationId, clinicId);
    }

    @Test
    void setClinicalReviewCreatesWhenNoneExists() {
        grant(AccessLevel.READ_WRITE);
        when(clinicalReviewRepository.findByClinicIdAndPatientIdAndKind(clinicId, patientId, ClinicalReviewKind.CONDITIONS))
                .thenReturn(Optional.empty());
        when(clinicalReviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.setClinicalReview(patientId, clinicId, userId, "Dra. Ruiz", ClinicalReviewKind.CONDITIONS, true);

        ArgumentCaptor<PatientClinicalReview> captor = ArgumentCaptor.forClass(PatientClinicalReview.class);
        verify(clinicalReviewRepository).save(captor.capture());
        PatientClinicalReview saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getKind()).isEqualTo(ClinicalReviewKind.CONDITIONS);
        assertThat(saved.isNoneReported()).isTrue();
        assertThat(saved.getReviewedByUserId()).isEqualTo(userId);
        assertThat(saved.getReviewedByUserName()).isEqualTo("Dra. Ruiz");
        assertThat(saved.getReviewedAt()).isNotNull();
    }

    @Test
    void setClinicalReviewDeniedWithoutWriteAccess() {
        grant(AccessLevel.READ_ONLY);
        assertThatThrownBy(() -> service.setClinicalReview(
                patientId, clinicId, userId, "Dra. Ruiz", ClinicalReviewKind.ALLERGIES, true))
                .isInstanceOf(ClinicAccessDeniedException.class);
        verify(clinicalReviewRepository, never()).save(any());
    }

    @Test
    void getSummaryReflectsReviewFlagsPerKind() {
        grant(AccessLevel.READ_ONLY);
        when(clinicalReviewRepository.findByClinicIdAndPatientId(clinicId, patientId)).thenReturn(List.of(
                PatientClinicalReview.builder().id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId)
                        .kind(ClinicalReviewKind.ALLERGIES).noneReported(true).reviewedByUserName("Dra. Ruiz")
                        .reviewedAt(LocalDateTime.now()).build(),
                PatientClinicalReview.builder().id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId)
                        .kind(ClinicalReviewKind.MEDICATIONS).noneReported(true)
                        .reviewedAt(LocalDateTime.now()).build()));

        ClinicalSummary summary = service.getSummary(patientId, clinicId, userId);

        assertThat(summary.allergiesReview().noneReported()).isTrue();
        assertThat(summary.allergiesReview().reviewedByUserName()).isEqualTo("Dra. Ruiz");
        assertThat(summary.medicationsReview().noneReported()).isTrue();
        assertThat(summary.conditionsReview().noneReported()).isFalse();
    }

    @Test
    void addMedicationDeniedWithoutAccess() {
        grant(AccessLevel.NONE);
        assertThatThrownBy(() -> service.addMedication(patientId, clinicId, userId, "Dra. Ruiz",
                new MedicationInput("Metformina", "850 mg", "cada 12h", true, null, null, null)))
                .isInstanceOf(ClinicAccessDeniedException.class);
        verify(medicationRepository, never()).save(any());
    }
}
