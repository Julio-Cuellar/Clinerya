package com.jclinical.clinics.domain.service;

import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.clinics.domain.model.ResponsibleDoctorSetup;
import com.jclinical.clinics.domain.ports.in.ManageClinicUseCase.ClinicSetupDetails;
import com.jclinical.clinics.domain.ports.out.ClinicRepositoryPort;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.DoctorProfile;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.DoctorProfileRepositoryPort;
import com.jclinical.staff.domain.service.ClinicStaffService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El dueno siempre administra su clinica (CLINIC_ADMIN). Al darla de alta indica quien es el
 * medico responsable —nombre y cedula obligatorios— y si el mismo atiende pacientes: solo en ese
 * caso cuenta como doctor (agenda, consultorios, expediente), y siempre con cedula.
 */
class ClinicOwnerAttendsPatientsTest {

    private final InMemoryClinicRepository clinicRepository = new InMemoryClinicRepository();
    private final InMemoryClinicStaffRepository staffRepository = new InMemoryClinicStaffRepository();
    private final InMemoryDoctorProfileRepository profileRepository = new InMemoryDoctorProfileRepository();
    private final UUID ownerUserId = UUID.randomUUID();

    // updateClinicalPractice solo usa los repositorios de personal y de perfiles medicos.
    private final ClinicStaffService clinicStaffService =
            new ClinicStaffService(staffRepository, profileRepository, null, null, null, null);
    private final ManageClinicService manageClinicService =
            new ManageClinicService(clinicRepository, staffRepository, profileRepository, null, clinicStaffService);
    private final OnboardClinicService onboardClinicService =
            new OnboardClinicService(clinicRepository, staffRepository, profileRepository);

    @Test
    void ownerWhoIsTheResponsibleDoctorAttendsPatientsWithThatCedula() {
        Clinic clinic = createClinic(new ResponsibleDoctorSetup(true, "Dra. Duena", "1234 567", null, null));

        ClinicStaff owner = owner(clinic);
        assertEquals(StaffRole.CLINIC_ADMIN, owner.getRole());
        assertTrue(owner.isPractitioner());
        assertEquals("1234567", clinic.getResponsibleDoctorProfessionalLicense());
        assertEquals("1234567", profileRepository.findByClinicStaffId(owner.getId()).orElseThrow().getCedulaProfesional());
    }

    @Test
    void ownerWhoIsAnotherDoctorAttendsPatientsWithTheirOwnCedula() {
        Clinic clinic = createClinic(new ResponsibleDoctorSetup(false, "Dr. Responsable", "7654321", true, "1122334"));

        ClinicStaff owner = owner(clinic);
        assertTrue(owner.isPractitioner());
        assertEquals("7654321", clinic.getResponsibleDoctorProfessionalLicense());
        assertEquals("1122334", profileRepository.findByClinicStaffId(owner.getId()).orElseThrow().getCedulaProfesional());
    }

    @Test
    void ownerWhoOnlyManagesIsNotAPractitioner() {
        Clinic clinic = createClinic(new ResponsibleDoctorSetup(false, "Dr. Responsable", "7654321", false, null));

        assertFalse(owner(clinic).isPractitioner());
        assertTrue(profileRepository.profiles.isEmpty());
    }

    @Test
    void responsibleDoctorAndTheOwnersChoiceAreRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> createClinic(new ResponsibleDoctorSetup(false, " ", "7654321", false, null)));
        assertThrows(IllegalArgumentException.class,
                () -> createClinic(new ResponsibleDoctorSetup(false, "Dr. Responsable", null, false, null)));
        assertThrows(IllegalArgumentException.class,
                () -> createClinic(new ResponsibleDoctorSetup(false, "Dr. Responsable", "7654321", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> createClinic(new ResponsibleDoctorSetup(false, "Dr. Responsable", "7654321", true, null)));

        assertTrue(clinicRepository.items.isEmpty());
        assertTrue(staffRepository.staff.isEmpty());
        assertTrue(profileRepository.profiles.isEmpty());
    }

    @Test
    void registrationLeavesTheOwnerManagingUntilTheSetupIsCompleted() {
        onboardClinicService.createPendingClinic(ownerUserId, "duena@clinerya.com", "Dra. Duena", "Clinica Duena");
        onboardClinicService.activateClinic(ownerUserId);
        Clinic clinic = clinicRepository.findByOwnerUserId(ownerUserId).get(0);
        assertFalse(owner(clinic).isPractitioner());

        Clinic completed = manageClinicService.completeSetup(ownerUserId, clinic.getId(), details("Clinica Duena"),
                new ResponsibleDoctorSetup(true, "Dra. Duena", "7654321", null, null));

        ClinicStaff owner = owner(completed);
        assertTrue(owner.isActive());
        assertTrue(owner.isPractitioner());
        assertEquals("Dra. Duena", completed.getResponsibleDoctorName());
        assertEquals("7654321", profileRepository.findByClinicStaffId(owner.getId()).orElseThrow().getCedulaProfesional());
    }

    @Test
    void setupWithAnInvalidCedulaSavesNothing() {
        onboardClinicService.createPendingClinic(ownerUserId, "duena@clinerya.com", "Dra. Duena", "Clinica Duena");
        onboardClinicService.activateClinic(ownerUserId);
        Clinic clinic = clinicRepository.findByOwnerUserId(ownerUserId).get(0);

        assertThrows(IllegalArgumentException.class, () -> manageClinicService.completeSetup(ownerUserId,
                clinic.getId(), details("Nombre nuevo"), new ResponsibleDoctorSetup(true, "Dra. Duena", "12AB", null, null)));

        assertEquals("Clinica Duena", clinicRepository.findById(clinic.getId()).orElseThrow().getName());
        assertFalse(owner(clinic).isPractitioner());
    }

    private Clinic createClinic(ResponsibleDoctorSetup responsibleDoctor) {
        return manageClinicService.createClinic(ownerUserId, "Clinica Duena", "duena@clinerya.com", null,
                null, null, null, null, null, null, null, null, null, null, responsibleDoctor);
    }

    private static ClinicSetupDetails details(String name) {
        return new ClinicSetupDetails(name, "duena@clinerya.com", null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    private ClinicStaff owner(Clinic clinic) {
        return staffRepository.findByClinicIdAndUserId(clinic.getId(), ownerUserId).orElseThrow();
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

    private static final class InMemoryClinicStaffRepository implements ClinicStaffRepositoryPort {
        private final List<ClinicStaff> staff = new ArrayList<>();

        @Override
        public ClinicStaff save(ClinicStaff value) {
            staff.removeIf(existing -> existing.getId().equals(value.getId()));
            staff.add(value);
            return value;
        }

        @Override
        public Optional<ClinicStaff> findById(UUID id) {
            return staff.stream().filter(value -> id.equals(value.getId())).findFirst();
        }

        @Override
        public Optional<ClinicStaff> findByClinicIdAndUserId(UUID clinicId, UUID userId) {
            return staff.stream()
                    .filter(value -> clinicId.equals(value.getClinicId()) && userId.equals(value.getUserId()))
                    .findFirst();
        }

        @Override
        public List<ClinicStaff> findByClinicId(UUID clinicId) {
            return staff.stream().filter(value -> clinicId.equals(value.getClinicId())).toList();
        }

        @Override
        public List<ClinicStaff> findByUserId(UUID userId) {
            return staff.stream().filter(value -> userId.equals(value.getUserId())).toList();
        }
    }

    private static final class InMemoryDoctorProfileRepository implements DoctorProfileRepositoryPort {
        private final List<DoctorProfile> profiles = new ArrayList<>();

        @Override
        public DoctorProfile save(DoctorProfile profile) {
            profiles.removeIf(existing -> existing.getId().equals(profile.getId()));
            profiles.add(profile);
            return profile;
        }

        @Override
        public Optional<DoctorProfile> findByClinicStaffId(UUID clinicStaffId) {
            return profiles.stream().filter(profile -> clinicStaffId.equals(profile.getClinicStaffId())).findFirst();
        }
    }
}
