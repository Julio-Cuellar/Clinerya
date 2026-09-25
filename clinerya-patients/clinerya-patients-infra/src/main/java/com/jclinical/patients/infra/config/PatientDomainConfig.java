package com.jclinical.patients.infra.config;

import com.jclinical.patients.domain.model.Address;
import com.jclinical.patients.domain.model.ConsentSource;
import com.jclinical.patients.domain.model.ContactConsent;
import com.jclinical.patients.domain.model.BloodType;
import com.jclinical.patients.domain.model.EmergencyContact;
import com.jclinical.patients.domain.model.Gender;
import com.jclinical.patients.domain.model.MaritalStatus;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.out.PatientRepositoryPort;
import com.jclinical.patients.domain.service.PatientService;
import com.jclinical.patients.infra.adapters.out.PatientEntity;
import com.jclinical.patients.infra.adapters.out.PatientMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PatientDomainConfig {

    @Bean
    public PatientService patientService(PatientRepositoryPort patientRepository) {
        return new PatientService(patientRepository);
    }

    @Bean
    @ConditionalOnMissingBean(PatientMapper.class)
    public PatientMapper patientMapper() {
        return new PatientMapper() {
            @Override
            public PatientEntity toEntity(Patient domain) {
                if (domain == null) {
                    return null;
                }
                Address address = domain.getAddress();
                EmergencyContact emergencyContact = domain.getEmergencyContact();
                ContactConsent consent = domain.getContactConsent() == null
                        ? ContactConsent.notRecorded() : domain.getContactConsent();
                return PatientEntity.builder()
                        .id(domain.getId())
                        .clinicId(domain.getClinicId())
                        .firstName(domain.getFirstName())
                        .lastNamePaterno(domain.getLastNamePaterno())
                        .lastNameMaterno(domain.getLastNameMaterno())
                        .curp(domain.getCurp())
                        .dateOfBirth(domain.getDateOfBirth())
                        .gender(toEnumValue(domain.getGender()))
                        .phone(domain.getPhone())
                        .email(domain.getEmail())
                        .occupation(domain.getOccupation())
                        .maritalStatus(toEnumValue(domain.getMaritalStatus()))
                        .nationality(domain.getNationality())
                        .bloodType(toEnumValue(domain.getBloodType()))
                        .addressStreet(address == null ? null : address.getStreet())
                        .addressOutdoorNumber(address == null ? null : address.getOutdoorNumber())
                        .addressIndoorNumber(address == null ? null : address.getIndoorNumber())
                        .addressColonia(address == null ? null : address.getColonia())
                        .addressMunicipality(address == null ? null : address.getMunicipality())
                        .addressState(address == null ? null : address.getState())
                        .addressZipCode(address == null ? null : address.getZipCode())
                        .emergencyContactFullName(emergencyContact == null ? null : emergencyContact.getFullName())
                        .emergencyContactRelationship(emergencyContact == null ? null : emergencyContact.getRelationship())
                        .emergencyContactPhone(emergencyContact == null ? null : emergencyContact.getPhone())
                        .contactConsentGranted(consent.granted())
                        .contactConsentTextVersion(consent.textVersion())
                        .contactConsentSource(toEnumValue(consent.source()))
                        .contactConsentRecordedBy(consent.recordedByUserId())
                        .contactConsentRecordedAt(consent.recordedAt())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .build();
            }

            @Override
            public Patient toDomain(PatientEntity entity) {
                if (entity == null) {
                    return null;
                }
                return Patient.builder()
                        .id(entity.getId())
                        .clinicId(entity.getClinicId())
                        .firstName(entity.getFirstName())
                        .lastNamePaterno(entity.getLastNamePaterno())
                        .lastNameMaterno(entity.getLastNameMaterno())
                        .curp(entity.getCurp())
                        .dateOfBirth(entity.getDateOfBirth())
                        .gender(toEnum(entity.getGender(), Gender.class))
                        .phone(entity.getPhone())
                        .email(entity.getEmail())
                        .occupation(entity.getOccupation())
                        .maritalStatus(toEnum(entity.getMaritalStatus(), MaritalStatus.class))
                        .nationality(entity.getNationality())
                        .bloodType(toEnum(entity.getBloodType(), BloodType.class))
                        .address(Address.builder()
                                .street(entity.getAddressStreet())
                                .outdoorNumber(entity.getAddressOutdoorNumber())
                                .indoorNumber(entity.getAddressIndoorNumber())
                                .colonia(entity.getAddressColonia())
                                .municipality(entity.getAddressMunicipality())
                                .state(entity.getAddressState())
                                .zipCode(entity.getAddressZipCode())
                                .build())
                        .emergencyContact(EmergencyContact.builder()
                                .fullName(entity.getEmergencyContactFullName())
                                .relationship(entity.getEmergencyContactRelationship())
                                .phone(entity.getEmergencyContactPhone())
                                .build())
                        .contactConsent(new ContactConsent(
                                entity.isContactConsentGranted(),
                                entity.getContactConsentTextVersion(),
                                toEnum(entity.getContactConsentSource(), ConsentSource.class),
                                entity.getContactConsentRecordedBy(),
                                entity.getContactConsentRecordedAt()))
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
            }
        };
    }

    private String toEnumValue(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private <E extends Enum<E>> E toEnum(String value, Class<E> type) {
        return value == null || value.isBlank() ? null : Enum.valueOf(type, value);
    }
}
