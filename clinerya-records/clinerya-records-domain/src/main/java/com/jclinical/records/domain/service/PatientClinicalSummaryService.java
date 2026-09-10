package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.AllergySeverity;
import com.jclinical.records.domain.model.AllergyCategory;
import com.jclinical.records.domain.model.ClinicalDataSource;
import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.ClinicalReviewKind;
import com.jclinical.records.domain.model.ConditionStatus;
import com.jclinical.records.domain.model.PatientAllergy;
import com.jclinical.records.domain.model.PatientClinicalReview;
import com.jclinical.records.domain.model.PatientCondition;
import com.jclinical.records.domain.model.PatientMedication;
import com.jclinical.records.domain.ports.in.ManagePatientClinicalSummaryUseCase;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.ports.out.PatientAllergyRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientClinicalReviewRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientConditionRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientLookupPort;
import com.jclinical.records.domain.ports.out.PatientMedicationRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PatientClinicalSummaryService implements ManagePatientClinicalSummaryUseCase {

    private final PatientAllergyRepositoryPort allergyRepository;
    private final PatientConditionRepositoryPort conditionRepository;
    private final PatientMedicationRepositoryPort medicationRepository;
    private final PatientClinicalReviewRepositoryPort clinicalReviewRepository;
    private final PatientValidatorPort patientValidator;
    private final PatientAccessAuthorizationPort accessAuthorizationPort;
    private final PatientLookupPort patientLookup;
    private final ClinicalNoteRepositoryPort noteRepository;

    public PatientClinicalSummaryService(PatientAllergyRepositoryPort allergyRepository,
                                         PatientConditionRepositoryPort conditionRepository,
                                         PatientMedicationRepositoryPort medicationRepository,
                                         PatientClinicalReviewRepositoryPort clinicalReviewRepository,
                                         PatientValidatorPort patientValidator,
                                         PatientAccessAuthorizationPort accessAuthorizationPort,
                                         PatientLookupPort patientLookup,
                                         ClinicalNoteRepositoryPort noteRepository) {
        this.allergyRepository = allergyRepository;
        this.conditionRepository = conditionRepository;
        this.medicationRepository = medicationRepository;
        this.clinicalReviewRepository = clinicalReviewRepository;
        this.patientValidator = patientValidator;
        this.accessAuthorizationPort = accessAuthorizationPort;
        this.patientLookup = patientLookup;
        this.noteRepository = noteRepository;
    }

    @Override
    public ClinicalSummary getSummary(UUID patientId, UUID clinicId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, false);

        String bloodType = patientLookup.findPatient(patientId)
                .map(PatientLookupPort.PatientDetails::bloodType)
                .orElse(null);

        List<PatientAllergy> allergies = allergyRepository.findByClinicIdAndPatientId(clinicId, patientId);
        List<PatientCondition> conditions = conditionRepository.findByClinicIdAndPatientId(clinicId, patientId);
        List<PatientMedication> activeMedications = medicationRepository.findByClinicIdAndPatientId(clinicId, patientId)
                .stream()
                .filter(PatientMedication::isActive)
                .toList();

        LocalDateTime lastNoteAt = noteRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId)
                .stream()
                .findFirst()
                .map(ClinicalNote::getCreatedAt)
                .orElse(null);

        Map<ClinicalReviewKind, ReviewStatus> reviews = new EnumMap<>(ClinicalReviewKind.class);
        for (PatientClinicalReview review : clinicalReviewRepository.findByClinicIdAndPatientId(clinicId, patientId)) {
            reviews.put(review.getKind(), new ReviewStatus(
                    review.isNoneReported(), review.getReviewedByUserName(), review.getReviewedAt()));
        }

        return new ClinicalSummary(patientId, bloodType, allergies, conditions, activeMedications, lastNoteAt,
                reviews.getOrDefault(ClinicalReviewKind.ALLERGIES, ReviewStatus.empty()),
                reviews.getOrDefault(ClinicalReviewKind.CONDITIONS, ReviewStatus.empty()),
                reviews.getOrDefault(ClinicalReviewKind.MEDICATIONS, ReviewStatus.empty()));
    }

    @Override
    public void setClinicalReview(UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName,
                                  ClinicalReviewKind kind, boolean noneReported) {
        authorize(requestingUserId, patientId, clinicId, true);
        PatientClinicalReview review = clinicalReviewRepository.findByClinicIdAndPatientIdAndKind(clinicId, patientId, kind)
                .orElseGet(() -> PatientClinicalReview.builder()
                        .id(UUID.randomUUID())
                        .clinicId(clinicId)
                        .patientId(patientId)
                        .kind(kind)
                        .build());
        review.setNoneReported(noneReported);
        review.setReviewedByUserId(requestingUserId);
        review.setReviewedByUserName(requestingUserName);
        review.setReviewedAt(LocalDateTime.now());
        clinicalReviewRepository.save(review);
    }

    // ---- Alergias ----------------------------------------------------------

    @Override
    public PatientAllergy addAllergy(UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, AllergyInput input) {
        authorize(requestingUserId, patientId, clinicId, true);
        PatientAllergy allergy = PatientAllergy.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .patientId(patientId)
                .substance(required(input.substance(), "La sustancia de la alergia es obligatoria."))
                .reaction(input.reaction())
                .severity(input.severity() != null ? input.severity() : AllergySeverity.UNKNOWN)
                .category(input.category() != null ? input.category() : AllergyCategory.DRUG)
                .source(ClinicalDataSource.MANUAL)
                .notedByUserId(requestingUserId)
                .notedByUserName(requestingUserName)
                .notedAt(LocalDateTime.now())
                .build();
        return allergyRepository.save(allergy);
    }

    @Override
    public PatientAllergy updateAllergy(UUID allergyId, UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, AllergyInput input) {
        authorize(requestingUserId, patientId, clinicId, true);
        PatientAllergy allergy = allergyRepository.findByIdAndClinicId(allergyId, clinicId)
                .filter(existing -> existing.getPatientId().equals(patientId))
                .orElseThrow(() -> new IllegalArgumentException("La alergia no existe para este paciente en esta clinica."));
        allergy.setSubstance(required(input.substance(), "La sustancia de la alergia es obligatoria."));
        allergy.setReaction(input.reaction());
        allergy.setSeverity(input.severity() != null ? input.severity() : AllergySeverity.UNKNOWN);
        allergy.setCategory(input.category() != null ? input.category() : AllergyCategory.DRUG);
        allergy.setNotedByUserId(requestingUserId);
        allergy.setNotedByUserName(requestingUserName);
        allergy.setNotedAt(LocalDateTime.now());
        return allergyRepository.save(allergy);
    }

    @Override
    public void removeAllergy(UUID allergyId, UUID patientId, UUID clinicId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, true);
        allergyRepository.findByIdAndClinicId(allergyId, clinicId)
                .filter(existing -> existing.getPatientId().equals(patientId))
                .orElseThrow(() -> new IllegalArgumentException("La alergia no existe para este paciente en esta clinica."));
        allergyRepository.deleteByIdAndClinicId(allergyId, clinicId);
    }

    // ---- Padecimientos ---------------------------------------------------------

    @Override
    public PatientCondition addCondition(UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, ConditionInput input) {
        authorize(requestingUserId, patientId, clinicId, true);
        PatientCondition condition = PatientCondition.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .patientId(patientId)
                .name(required(input.name(), "El nombre del padecimiento es obligatorio."))
                .icd10Code(input.icd10Code())
                .status(input.status() != null ? input.status() : ConditionStatus.ACTIVE)
                .onsetDate(input.onsetDate())
                .source(ClinicalDataSource.MANUAL)
                .notedByUserId(requestingUserId)
                .notedByUserName(requestingUserName)
                .notedAt(LocalDateTime.now())
                .build();
        return conditionRepository.save(condition);
    }

    @Override
    public PatientCondition updateCondition(UUID conditionId, UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, ConditionInput input) {
        authorize(requestingUserId, patientId, clinicId, true);
        PatientCondition condition = conditionRepository.findByIdAndClinicId(conditionId, clinicId)
                .filter(existing -> existing.getPatientId().equals(patientId))
                .orElseThrow(() -> new IllegalArgumentException("El padecimiento no existe para este paciente en esta clinica."));
        condition.setName(required(input.name(), "El nombre del padecimiento es obligatorio."));
        condition.setIcd10Code(input.icd10Code());
        condition.setStatus(input.status() != null ? input.status() : ConditionStatus.ACTIVE);
        condition.setOnsetDate(input.onsetDate());
        condition.setNotedByUserId(requestingUserId);
        condition.setNotedByUserName(requestingUserName);
        condition.setNotedAt(LocalDateTime.now());
        return conditionRepository.save(condition);
    }

    @Override
    public void removeCondition(UUID conditionId, UUID patientId, UUID clinicId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, true);
        conditionRepository.findByIdAndClinicId(conditionId, clinicId)
                .filter(existing -> existing.getPatientId().equals(patientId))
                .orElseThrow(() -> new IllegalArgumentException("El padecimiento no existe para este paciente en esta clinica."));
        conditionRepository.deleteByIdAndClinicId(conditionId, clinicId);
    }

    // ---- Medicacion ----------------------------------------------------------

    @Override
    public PatientMedication addMedication(UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, MedicationInput input) {
        authorize(requestingUserId, patientId, clinicId, true);
        PatientMedication medication = PatientMedication.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .patientId(patientId)
                .medicationName(required(input.medicationName(), "El nombre del medicamento es obligatorio."))
                .dose(input.dose())
                .schedule(input.schedule())
                .active(input.active())
                .startedOn(input.startedOn())
                .stoppedOn(input.stoppedOn())
                .prescriptionId(input.prescriptionId())
                .source(ClinicalDataSource.MANUAL)
                .notedByUserId(requestingUserId)
                .notedByUserName(requestingUserName)
                .notedAt(LocalDateTime.now())
                .build();
        return medicationRepository.save(medication);
    }

    @Override
    public PatientMedication updateMedication(UUID medicationId, UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, MedicationInput input) {
        authorize(requestingUserId, patientId, clinicId, true);
        PatientMedication medication = medicationRepository.findByIdAndClinicId(medicationId, clinicId)
                .filter(existing -> existing.getPatientId().equals(patientId))
                .orElseThrow(() -> new IllegalArgumentException("El medicamento no existe para este paciente en esta clinica."));
        medication.setMedicationName(required(input.medicationName(), "El nombre del medicamento es obligatorio."));
        medication.setDose(input.dose());
        medication.setSchedule(input.schedule());
        medication.setActive(input.active());
        medication.setStartedOn(input.startedOn());
        medication.setStoppedOn(input.stoppedOn());
        medication.setPrescriptionId(input.prescriptionId());
        medication.setNotedByUserId(requestingUserId);
        medication.setNotedByUserName(requestingUserName);
        medication.setNotedAt(LocalDateTime.now());
        return medicationRepository.save(medication);
    }

    @Override
    public void removeMedication(UUID medicationId, UUID patientId, UUID clinicId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, true);
        medicationRepository.findByIdAndClinicId(medicationId, clinicId)
                .filter(existing -> existing.getPatientId().equals(patientId))
                .orElseThrow(() -> new IllegalArgumentException("El medicamento no existe para este paciente en esta clinica."));
        medicationRepository.deleteByIdAndClinicId(medicationId, clinicId);
    }

    // ---- helpers ---------------------------------------------------------------

    private void authorize(UUID requestingUserId, UUID patientId, UUID clinicId, boolean requireWrite) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clinica.");
        }
        AccessDecision decision = accessAuthorizationPort.resolveAccess(requestingUserId, clinicId, patientId);
        if (decision.level() == AccessLevel.NONE || (requireWrite && decision.level() != AccessLevel.READ_WRITE)) {
            throw new ClinicAccessDeniedException("No tienes acceso a este expediente.");
        }
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
