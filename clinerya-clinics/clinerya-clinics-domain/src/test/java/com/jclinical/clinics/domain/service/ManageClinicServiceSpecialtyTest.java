package com.jclinical.clinics.domain.service;

import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.clinics.domain.ports.out.ClinicRepositoryPort;
import com.jclinical.core.domain.ClinicSpecialty;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Cambiar la especialidad reconfigura el módulo de tratamientos entero (vocabulario, qué campos se
 * capturan, qué plantilla se recomienda), así que no basta con pertenecer a la clínica: hace falta
 * ser el dueño o tener MANAGE_CLINIC.
 */
class ManageClinicServiceSpecialtyTest {

    private final InMemoryClinicRepository clinicRepository = new InMemoryClinicRepository();
    private final UUID clinicId = UUID.randomUUID();
    private final UUID ownerUserId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    private boolean grantsManageClinic;
    private final StaffPermissionCheckerPort permissionChecker =
            (clinic, user, permission) -> grantsManageClinic && permission == StaffPermission.MANAGE_CLINIC;

    private ManageClinicService service;

    @BeforeEach
    void setUp() {
        grantsManageClinic = false;
        service = new ManageClinicService(clinicRepository, null, null, permissionChecker, null);
        clinicRepository.save(Clinic.builder()
                .id(clinicId)
                .ownerUserId(ownerUserId)
                .name("Clínica de prueba")
                .specialty(ClinicSpecialty.ODONTOLOGIA)
                .build());
    }

    @Test
    void ownerCanChangeSpecialty() {
        Clinic updated = service.updateSpecialty(ownerUserId, clinicId, ClinicSpecialty.NUTRICION);

        assertEquals(ClinicSpecialty.NUTRICION, updated.getSpecialty());
        assertEquals(ClinicSpecialty.NUTRICION, clinicRepository.findById(clinicId).orElseThrow().getSpecialty());
    }

    @Test
    void staffWithManageClinicCanChangeSpecialty() {
        grantsManageClinic = true;

        Clinic updated = service.updateSpecialty(otherUserId, clinicId, ClinicSpecialty.FISIOTERAPIA);

        assertEquals(ClinicSpecialty.FISIOTERAPIA, updated.getSpecialty());
    }

    @Test
    void staffWithoutManageClinicIsRejected() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.updateSpecialty(otherUserId, clinicId, ClinicSpecialty.NUTRICION));

        assertEquals(ClinicSpecialty.ODONTOLOGIA, clinicRepository.findById(clinicId).orElseThrow().getSpecialty());
    }

    @Test
    void specialtyIsRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateSpecialty(ownerUserId, clinicId, null));

        assertEquals(ClinicSpecialty.ODONTOLOGIA, clinicRepository.findById(clinicId).orElseThrow().getSpecialty());
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
