package com.jclinical.clinics.domain.service;

import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.clinics.domain.ports.in.GetClinicPublicProfileUseCase.ClinicPublicProfile;
import com.jclinical.clinics.domain.ports.out.ClinicRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El perfil publico es lo unico de la clinica que se le puede decir a alguien que todavia no es
 * paciente (asistente de WhatsApp): nombre, direccion, telefono, correo y aviso de privacidad.
 */
class ManageClinicServicePublicProfileTest {

    private final InMemoryClinicRepository clinicRepository = new InMemoryClinicRepository();
    private final UUID clinicId = UUID.randomUUID();

    private ManageClinicService service;

    @BeforeEach
    void setUp() {
        service = new ManageClinicService(clinicRepository, null, null, null, null);
    }

    @Test
    void thePublicProfileHasWhatAPatientNeedsToFindAndReachTheClinic() {
        clinicRepository.save(Clinic.builder()
                .id(clinicId)
                .ownerUserId(UUID.randomUUID())
                .name("Clínica Sonrisa")
                .legalName("Sonrisa Dental SA de CV")
                .rfc("SDE010101AAA")
                .addressStreet("Av. Juárez 120")
                .addressColonia("Centro")
                .addressMunicipality("Puebla")
                .addressState("Puebla")
                .addressZip("72000")
                .phone("222 555 0101")
                .email("contacto@sonrisa.mx")
                .privacyNoticeUrl("https://sonrisa.mx/privacidad")
                .build());

        ClinicPublicProfile profile = service.getPublicProfile(clinicId).orElseThrow();

        assertEquals(new ClinicPublicProfile(clinicId, "Clínica Sonrisa",
                "Av. Juárez 120, Col. Centro, Puebla, Puebla, C.P. 72000", "222 555 0101", "contacto@sonrisa.mx",
                "https://sonrisa.mx/privacidad"), profile);
    }

    @Test
    void missingAddressPartsAreSkipped() {
        clinicRepository.save(Clinic.builder().id(clinicId).ownerUserId(UUID.randomUUID()).name("Consultorio")
                .addressStreet("Calle 5 #10").addressMunicipality(" ").addressState("Jalisco").build());

        assertEquals("Calle 5 #10, Jalisco", service.getPublicProfile(clinicId).orElseThrow().address());
    }

    @Test
    void aClinicWithoutAddressOrContactDataStillHasItsName() {
        clinicRepository.save(Clinic.builder().id(clinicId).ownerUserId(UUID.randomUUID()).name("Consultorio").build());

        ClinicPublicProfile profile = service.getPublicProfile(clinicId).orElseThrow();

        assertEquals("Consultorio", profile.name());
        assertNull(profile.address());
        assertNull(profile.phone());
        assertNull(profile.privacyNoticeUrl());
    }

    @Test
    void anUnknownClinicHasNoProfile() {
        assertTrue(service.getPublicProfile(UUID.randomUUID()).isEmpty());
    }

    private static final class InMemoryClinicRepository implements ClinicRepositoryPort {
        private final List<Clinic> items = new ArrayList<>();

        @Override
        public Clinic save(Clinic clinic) {
            items.removeIf(existing -> existing.getId().equals(clinic.getId()));
            items.add(clinic);
            return clinic;
        }

        @Override
        public Optional<Clinic> findById(UUID id) {
            return items.stream().filter(item -> item.getId().equals(id)).findFirst();
        }

        @Override
        public List<Clinic> findByOwnerUserId(UUID ownerUserId) {
            return items.stream().filter(item -> item.getOwnerUserId().equals(ownerUserId)).toList();
        }
    }
}
