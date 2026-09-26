package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.ports.in.ManageClinicScheduleUseCase;
import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.model.ClinicInfo.OpeningHours;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort.NewPatient;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort.Sex;
import com.jclinical.clinics.domain.ports.in.GetClinicPublicProfileUseCase.ClinicPublicProfile;
import com.jclinical.patients.domain.model.ConsentSource;
import com.jclinical.patients.domain.model.ContactConsentText;
import com.jclinical.patients.domain.model.Gender;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.RecordContactConsentUseCase.ContactConsentDecision;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase.RegisterPatientCommand;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las dos puertas de la automatizacion hacia pacientes y clinica para atender a un numero nuevo. */
class NewPatientAdaptersTest {

    private final UUID clinicId = UUID.randomUUID();

    @Test
    void aNewPatientIsRegisteredWithTheNationalPhoneAndTheirOwnConsent() {
        List<RegisterPatientCommand> commands = new ArrayList<>();
        List<Object[]> consents = new ArrayList<>();
        UUID createdId = UUID.randomUUID();
        PatientRegistrationAdapter adapter = new PatientRegistrationAdapter(
                command -> {
                    commands.add(command);
                    return Patient.builder().id(createdId).clinicId(command.clinicId()).firstName(command.firstName())
                            .lastNamePaterno(command.lastNamePaterno()).phone(command.phone()).build();
                },
                (patientId, clinic, decision, source) -> {
                    consents.add(new Object[]{patientId, decision, source});
                    return null;
                });

        UUID id = adapter.register(new NewPatient(clinicId, "Juan", "Pérez", "López", LocalDate.of(1990, 3, 14), Sex.MALE,
                "5215588887777", "juan@correo.com", ContactConsentText.CURRENT_VERSION));

        assertEquals(createdId, id);
        RegisterPatientCommand command = commands.get(0);
        assertEquals("5588887777", command.phone(), "como el resto de pacientes: 10 digitos, sin lada de WhatsApp");
        assertEquals(Gender.MALE, command.gender());
        assertEquals(LocalDate.of(1990, 3, 14), command.dateOfBirth());
        assertEquals("juan@correo.com", command.email());
        assertEquals("López", command.lastNameMaterno());
        assertNull(command.contactConsent(), "el alta no lo marca como capturado por el personal");
        assertEquals(createdId, consents.get(0)[0]);
        assertEquals(new ContactConsentDecision(true, ContactConsentText.CURRENT_VERSION, null), consents.get(0)[1]);
        assertEquals(ConsentSource.WHATSAPP_CHAT, consents.get(0)[2], "lo autorizo el propio paciente en el chat");
    }

    @Test
    void theConsentTextIsTheSameOneReadAtTheFrontDesk() {
        PatientRegistrationAdapter adapter = new PatientRegistrationAdapter(command -> null, (p, c, d, s) -> null);

        assertEquals(ContactConsentText.CURRENT_VERSION, adapter.consentText().version());
        assertEquals(ContactConsentText.CURRENT_TEXT, adapter.consentText().text());
    }

    @Test
    void clinicInfoJoinsThePublicProfileWithTheWeeklySchedule() {
        ClinicInfoAdapter adapter = new ClinicInfoAdapter(
                clinic -> Optional.of(new ClinicPublicProfile(clinic, "Clínica Sonrisa", "Av. Juárez 120", "222 555 0101",
                        null, "https://sonrisa.mx/privacidad")),
                scheduleOf(List.of(
                        day(DayOfWeek.SATURDAY, true, LocalTime.of(9, 0), LocalTime.of(14, 0)),
                        day(DayOfWeek.MONDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                        day(DayOfWeek.SUNDAY, false, null, null))));

        ClinicInfo info = adapter.find(clinicId).orElseThrow();

        assertEquals("Clínica Sonrisa", info.name());
        assertEquals("https://sonrisa.mx/privacidad", info.privacyNoticeUrl());
        assertEquals(List.of(
                new OpeningHours(DayOfWeek.MONDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                new OpeningHours(DayOfWeek.SATURDAY, true, LocalTime.of(9, 0), LocalTime.of(14, 0)),
                new OpeningHours(DayOfWeek.SUNDAY, false, null, null)), info.hours());
    }

    @Test
    void anUnknownClinicHasNoInfo() {
        ClinicInfoAdapter adapter = new ClinicInfoAdapter(clinic -> Optional.empty(), scheduleOf(List.of()));

        assertTrue(adapter.find(clinicId).isEmpty());
    }

    private ClinicSchedule day(DayOfWeek dayOfWeek, boolean open, LocalTime start, LocalTime end) {
        return ClinicSchedule.builder().id(UUID.randomUUID()).clinicId(clinicId).dayOfWeek(dayOfWeek).open(open)
                .startTime(start).endTime(end).build();
    }

    private static ManageClinicScheduleUseCase scheduleOf(List<ClinicSchedule> days) {
        return new ManageClinicScheduleUseCase() {
            @Override
            public List<ClinicSchedule> getSchedule(UUID actingUserId, UUID clinicId) {
                return days;
            }

            @Override
            public List<ClinicSchedule> updateSchedule(UUID actingUserId, UUID clinicId, List<DayScheduleCommand> days) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
