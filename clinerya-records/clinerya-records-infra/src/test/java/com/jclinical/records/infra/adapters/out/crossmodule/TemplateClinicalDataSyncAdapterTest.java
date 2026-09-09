package com.jclinical.records.infra.adapters.out.crossmodule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.records.domain.model.AllergyCategory;
import com.jclinical.records.domain.model.ClinicalDataSource;
import com.jclinical.records.domain.model.PatientAllergy;
import com.jclinical.records.domain.model.PatientCondition;
import com.jclinical.records.domain.model.PatientMedication;
import com.jclinical.records.domain.ports.out.PatientAllergyRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientConditionRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientMedicationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TemplateClinicalDataSyncAdapterTest {

    @Mock private PatientAllergyRepositoryPort allergyRepository;
    @Mock private PatientConditionRepositoryPort conditionRepository;
    @Mock private PatientMedicationRepositoryPort medicationRepository;

    private TemplateClinicalDataSyncAdapter adapter;

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new TemplateClinicalDataSyncAdapter(new ObjectMapper(),
                allergyRepository, conditionRepository, medicationRepository);
    }

    private void run(String schema, String answers) {
        adapter.sync(clinicId, patientId, schema, answers, userId, "Dra. Ruiz");
    }

    @Test
    void doesNothingWhenNoClinicalMapping() {
        run("""
            {"pages":[{"elements":[{"id":"alergias","type":"table"}]}]}
            """, """
            {"alergias":"[[\\"Penicilina\\",\\"Urticaria\\"]]"}
            """);
        verifyNoInteractions(allergyRepository, conditionRepository, medicationRepository);
    }

    @Test
    void doesNothingOnMalformedSchema() {
        run("{not json", "{\"a\":\"b\"}");
        verifyNoInteractions(allergyRepository, conditionRepository, medicationRepository);
    }

    @Test
    void syncsAllergiesFromMappedTableReplacingTemplateRows() {
        run("""
            {"pages":[{"elements":[
              {"id":"alergias","type":"table",
               "clinicalMapping":{"target":"ALLERGY","primary":0,"secondary":1}}
            ]}]}
            """, """
            {"alergias":"[[\\"Penicilina\\",\\"Urticaria\\"],[\\"Latex\\",\\"\\"],[\\"\\",\\"ignora fila sin sustancia\\"]]"}
            """);

        verify(allergyRepository).deleteByClinicIdAndPatientIdAndSource(clinicId, patientId, ClinicalDataSource.TEMPLATE);
        ArgumentCaptor<PatientAllergy> captor = ArgumentCaptor.forClass(PatientAllergy.class);
        verify(allergyRepository, times(2)).save(captor.capture());
        List<PatientAllergy> saved = captor.getAllValues();
        assertThat(saved).extracting(PatientAllergy::getSubstance).containsExactly("Penicilina", "Latex");
        assertThat(saved.get(0).getReaction()).isEqualTo("Urticaria");
        assertThat(saved.get(1).getReaction()).isNull();
        assertThat(saved).allMatch(a -> a.getSource() == ClinicalDataSource.TEMPLATE);
        assertThat(saved).allMatch(a -> a.getCategory() == AllergyCategory.OTHER);
        assertThat(saved).allMatch(a -> a.getNotedByUserId().equals(userId));
        verifyNoInteractions(conditionRepository, medicationRepository);
    }

    @Test
    void appliesDefaultCategoryWhenGiven() {
        run("""
            {"elements":[{"id":"alergiasFarmacos","type":"table",
              "clinicalMapping":{"target":"ALLERGY","defaultCategory":"DRUG"}}]}
            """, """
            {"alergiasFarmacos":"[[\\"Ibuprofeno\\"]]"}
            """);
        ArgumentCaptor<PatientAllergy> captor = ArgumentCaptor.forClass(PatientAllergy.class);
        verify(allergyRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(AllergyCategory.DRUG);
    }

    @Test
    void syncsConditionsWithIcd10AndOnsetDate() {
        run("""
            {"pages":[{"elements":[{"id":"padecimientos","type":"table",
              "clinicalMapping":{"target":"CONDITION","primary":0,"secondary":1,"tertiary":2}}]}]}
            """, """
            {"padecimientos":"[[\\"Diabetes tipo 2\\",\\"E11.9\\",\\"2020-01-15\\"],[\\"Hipertension\\",\\"\\",\\"no es fecha\\"]]"}
            """);
        verify(conditionRepository).deleteByClinicIdAndPatientIdAndSource(clinicId, patientId, ClinicalDataSource.TEMPLATE);
        ArgumentCaptor<PatientCondition> captor = ArgumentCaptor.forClass(PatientCondition.class);
        verify(conditionRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getIcd10Code()).isEqualTo("E11.9");
        assertThat(captor.getAllValues().get(0).getOnsetDate()).isEqualTo(LocalDate.of(2020, 1, 15));
        assertThat(captor.getAllValues().get(1).getIcd10Code()).isNull();
        assertThat(captor.getAllValues().get(1).getOnsetDate()).isNull();
    }

    @Test
    void syncsMedicationsAsActive() {
        run("""
            {"elements":[{"id":"meds","type":"table",
              "clinicalMapping":{"target":"MEDICATION","primary":0,"secondary":1,"tertiary":2}}]}
            """, """
            {"meds":"[[\\"Metformina\\",\\"850 mg\\",\\"cada 12h\\"]]"}
            """);
        ArgumentCaptor<PatientMedication> captor = ArgumentCaptor.forClass(PatientMedication.class);
        verify(medicationRepository).save(captor.capture());
        PatientMedication m = captor.getValue();
        assertThat(m.getMedicationName()).isEqualTo("Metformina");
        assertThat(m.getDose()).isEqualTo("850 mg");
        assertThat(m.getSchedule()).isEqualTo("cada 12h");
        assertThat(m.isActive()).isTrue();
        assertThat(m.getSource()).isEqualTo(ClinicalDataSource.TEMPLATE);
    }

    @Test
    void deletesTemplateRowsEvenWhenMappedTableIsEmpty() {
        run("""
            {"elements":[{"id":"alergias","type":"table","clinicalMapping":{"target":"ALLERGY"}}]}
            """, """
            {"alergias":"[]"}
            """);
        verify(allergyRepository).deleteByClinicIdAndPatientIdAndSource(clinicId, patientId, ClinicalDataSource.TEMPLATE);
        verify(allergyRepository, never()).save(any());
    }

    @Test
    void readsSingleValueFromNonTableField() {
        run("""
            {"elements":[{"id":"alergiaLibre","type":"text","clinicalMapping":{"target":"ALLERGY"}}]}
            """, """
            {"alergiaLibre":"Sulfas"}
            """);
        ArgumentCaptor<PatientAllergy> captor = ArgumentCaptor.forClass(PatientAllergy.class);
        verify(allergyRepository).save(captor.capture());
        assertThat(captor.getValue().getSubstance()).isEqualTo("Sulfas");
        verifyNoInteractions(conditionRepository, medicationRepository);
    }
}
