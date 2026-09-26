package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.model.ClinicInfo.OpeningHours;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.RegistrationDraft;
import com.jclinical.automation.domain.ports.out.ClinicInfoPort;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort;
import com.jclinical.automation.domain.ports.out.RegistrationDraftPort;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakeDoctors;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakeInterpreter;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakePatients;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakeRequests;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakeSlots;
import com.jclinical.automation.domain.service.ConversationServiceTest.InMemoryConversations;
import com.jclinical.automation.domain.service.ConversationServiceTest.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CU-4: un numero que no esta en los registros tambien se atiende. Puede pedir informacion de la
 * clinica (datos reales, sin IA) o registrarse como paciente nuevo: aviso y consentimiento, nombre,
 * apellidos, fecha de nacimiento y correo opcional; despues sigue la cita como cualquier paciente.
 */
class ConversationNewPatientTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 28, 9, 0);
    private static final String NEW_PHONE = "5215588887777";
    private static final String KNOWN_PHONE = "5215512345678";

    private final UUID clinicId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final FakePatients patients = new FakePatients();
    private final FakeDoctors doctors = new FakeDoctors();
    private final FakeSlots slots = new FakeSlots();
    private final FakeRequests requests = new FakeRequests();
    private final FakeInterpreter interpreter = new FakeInterpreter();
    private final InMemoryConversations conversations = new InMemoryConversations();
    private final MutableClock clock = new MutableClock(NOW);
    private final FakeClinicInfo clinicInfo = new FakeClinicInfo();
    private final FakeRegistrations registrations = new FakeRegistrations();
    private final InMemoryDrafts drafts = new InMemoryDrafts();

    private ConversationService service;

    @BeforeEach
    void setUp() {
        service = new ConversationService(conversations, patients, doctors, slots, requests, interpreter, clinicInfo,
                new NewPatientRegistration(conversations, registrations, drafts, clock), clock);
        patients.add(KNOWN_PHONE, UUID.randomUUID(), "Ana López");
        doctors.add(doctorId, "Dra. Beatriz Ramos");
        slots.add(doctorId, NOW.plusDays(1).withHour(10));
    }

    // ---- bienvenida e informacion -------------------------------------------------------------

    @Test
    void anUnknownNumberIsWelcomedInsteadOfTurnedAway() {
        OutboundReply reply = send("Hola");

        assertEquals(ConversationState.MENU, current().state());
        assertNull(current().patientId());
        assertTrue(reply.text().contains("Clínica Sonrisa"), reply.text());
        assertEquals(List.of(ConversationService.BOOK, ConversationService.CLINIC_INFO), optionIds(reply));
    }

    @Test
    void clinicInfoComesFromTheClinicsRealData() {
        send("Hola");

        OutboundReply reply = send(null, ConversationService.CLINIC_INFO);

        assertTrue(reply.text().contains("Av. Juárez 120, Col. Centro, Puebla"), reply.text());
        assertTrue(reply.text().contains("222 555 0101"), reply.text());
        assertTrue(reply.text().contains("Lunes a viernes: 09:00 a 18:00"), reply.text());
        assertTrue(reply.text().contains("Sábado: 09:00 a 14:00"), reply.text());
        assertTrue(reply.text().contains("Domingo: cerrado"), reply.text());
        assertEquals(ConversationState.MENU, current().state(), "despues de informar se puede seguir");
        assertEquals(List.of(ConversationService.BOOK, ConversationService.CLINIC_INFO), optionIds(reply));
        assertTrue(interpreter.calls.isEmpty(), "la informacion no pasa por la IA: no puede inventar");
    }

    @Test
    void aKnownPatientCanAlsoAskForClinicInfo() {
        OutboundReply reply = send(KNOWN_PHONE, "Hola", null);

        assertTrue(reply.text().contains("Ana"), reply.text());
        assertEquals(List.of(ConversationService.BOOK, ConversationService.CLINIC_INFO), optionIds(reply));
    }

    // ---- registro -----------------------------------------------------------------------------

    @Test
    void bookingFromAnUnknownNumberAsksForConsentFirst() {
        send("Hola");

        OutboundReply reply = send(null, ConversationService.BOOK);

        assertEquals(ConversationState.REGISTRO_CONSENTIMIENTO, current().state());
        assertTrue(reply.text().contains(FakeRegistrations.CONSENT_TEXT), reply.text());
        assertTrue(reply.text().contains("https://sonrisa.mx/privacidad"), "incluye el aviso de privacidad");
        assertEquals(List.of(NewPatientRegistration.ACCEPT, NewPatientRegistration.DECLINE), optionIds(reply));
    }

    @Test
    void withoutConsentNothingIsKeptAndTheMenuComesBack() {
        send("Hola");
        send(null, ConversationService.BOOK);

        OutboundReply reply = send(null, NewPatientRegistration.DECLINE);

        assertEquals(ConversationState.MENU, current().state());
        assertTrue(drafts.byConversation.isEmpty());
        assertTrue(registrations.registered.isEmpty());
        assertEquals(List.of(ConversationService.BOOK, ConversationService.CLINIC_INFO), optionIds(reply));
    }

    @Test
    void aNewPatientRegistersAndGoesOnToChooseTheDoctor() {
        send("Hola");
        send(null, ConversationService.BOOK);
        assertTrue(send(null, NewPatientRegistration.ACCEPT).text().contains("nombre"));
        assertTrue(send("  Juan  ").text().contains("apellidos"));
        assertTrue(send("Pérez López").text().contains("nacimiento"));
        OutboundReply askSex = send("14/03/1990");
        assertTrue(askSex.text().contains("sexo"), askSex.text());
        assertEquals(List.of(NewPatientRegistration.SEX_FEMALE, NewPatientRegistration.SEX_MALE,
                NewPatientRegistration.SEX_OTHER), optionIds(askSex));
        assertTrue(send(null, NewPatientRegistration.SEX_MALE).text().contains("correo"));

        OutboundReply reply = send("juan@correo.com");

        assertEquals(1, registrations.registered.size());
        PatientRegistrationPort.NewPatient patient = registrations.registered.get(0);
        assertEquals(new PatientRegistrationPort.NewPatient(clinicId, "Juan", "Pérez", "López", LocalDate.of(1990, 3, 14),
                PatientRegistrationPort.Sex.MALE, NEW_PHONE, "juan@correo.com", FakeRegistrations.CONSENT_VERSION), patient);
        assertEquals(registrations.lastId, current().patientId());
        assertEquals("Juan Pérez", current().patientName());
        assertEquals(ConversationState.ELEGIR_MEDICO, current().state(), "sigue la cita como cualquier paciente");
        assertTrue(reply.text().startsWith("Gracias, Juan"), reply.text());
        assertTrue(drafts.byConversation.isEmpty(), "el borrador se descarta al registrar");
        assertTrue(interpreter.calls.isEmpty(), "los datos personales nunca se mandan a la IA");
    }

    @Test
    void theEmailIsOptional() {
        registerUpToEmail();

        send(null, NewPatientRegistration.NO_EMAIL);

        assertNull(registrations.registered.get(0).email());
        assertEquals(ConversationState.ELEGIR_MEDICO, current().state());
    }

    @Test
    void anImpossibleOrFutureBirthDateIsAskedAgain() {
        registerUpTo("Pérez López");

        for (String wrong : List.of("31/02/1990", "1990", "hace mucho", "14/03/2030", "14/03/1850")) {
            OutboundReply reply = send(wrong);
            assertEquals(ConversationState.REGISTRO_NACIMIENTO, current().state(), wrong);
            assertTrue(reply.text().contains("dd/mm/aaaa"), reply.text());
        }
        send("1-3-1990");
        assertEquals(ConversationState.REGISTRO_SEXO, current().state(), "acepta guiones y un solo digito");
    }

    @Test
    void theSexIsChosenWithButtonsOnly() {
        registerUpTo("Pérez López");
        send("14/03/1990");

        OutboundReply reply = send("masculino");

        assertEquals(ConversationState.REGISTRO_SEXO, current().state(), "texto libre no cuenta: solo botones");
        assertEquals(3, reply.options().size());
        send(null, NewPatientRegistration.SEX_FEMALE);
        assertEquals(ConversationState.REGISTRO_CORREO, current().state());
    }

    @Test
    void anInvalidEmailIsAskedAgain() {
        registerUpToEmail();

        OutboundReply reply = send("juan arroba correo");

        assertEquals(ConversationState.REGISTRO_CORREO, current().state());
        assertTrue(registrations.registered.isEmpty());
        assertTrue(optionIds(reply).contains(NewPatientRegistration.NO_EMAIL));
    }

    @Test
    void compoundSurnamesAreKeptTogether() {
        registerUpTo("de la Cruz Pérez");
        send("14/03/1990");
        send(null, NewPatientRegistration.SEX_FEMALE);
        send(null, NewPatientRegistration.NO_EMAIL);

        assertEquals("de la Cruz", registrations.registered.get(0).lastNamePaterno());
        assertEquals("Pérez", registrations.registered.get(0).lastNameMaterno());
    }

    @Test
    void aSingleSurnameIsEnough() {
        registerUpTo("Pérez");
        send("14/03/1990");
        send(null, NewPatientRegistration.SEX_OTHER);
        send(null, NewPatientRegistration.NO_EMAIL);

        assertEquals("Pérez", registrations.registered.get(0).lastNamePaterno());
        assertNull(registrations.registered.get(0).lastNameMaterno());
    }

    @Test
    void anEmptyOrTooLongNameIsAskedAgain() {
        send("Hola");
        send(null, ConversationService.BOOK);
        send(null, NewPatientRegistration.ACCEPT);

        send("x".repeat(90));
        assertEquals(ConversationState.REGISTRO_NOMBRE, current().state());
        send("1234");
        assertEquals(ConversationState.REGISTRO_NOMBRE, current().state());
    }

    @Test
    void menuDuringRegistrationStartsOverAndDiscardsTheDraft() {
        registerUpTo("Pérez López");

        OutboundReply reply = send("menu");

        assertEquals(ConversationState.MENU, current().state());
        assertTrue(drafts.byConversation.isEmpty());
        assertEquals(List.of(ConversationService.BOOK, ConversationService.CLINIC_INFO), optionIds(reply));
    }

    // ---- utilidades -------------------------------------------------------------------------

    private void registerUpTo(String lastNames) {
        send("Hola");
        send(null, ConversationService.BOOK);
        send(null, NewPatientRegistration.ACCEPT);
        send("Juan");
        send(lastNames);
    }

    private void registerUpToEmail() {
        registerUpTo("Pérez López");
        send("14/03/1990");
        send(null, NewPatientRegistration.SEX_FEMALE);
    }

    private OutboundReply send(String text) {
        return send(NEW_PHONE, text, null);
    }

    private OutboundReply send(String text, String optionId) {
        return send(NEW_PHONE, text, optionId);
    }

    private OutboundReply send(String phone, String text, String optionId) {
        List<OutboundReply> replies = service.handle(new InboundMessage(clinicId, phone, text, optionId, clock.now()));
        assertEquals(1, replies.size());
        return replies.get(0);
    }

    private Conversation current() {
        return conversations.findActive(clinicId, NEW_PHONE).orElseThrow();
    }

    private static List<String> optionIds(OutboundReply reply) {
        return reply.options().stream().map(ConversationOption::id).toList();
    }

    // ---- dobles de prueba ---------------------------------------------------------------------

    static final class FakeClinicInfo implements ClinicInfoPort {
        @Override
        public Optional<ClinicInfo> find(UUID clinicId) {
            List<OpeningHours> week = new ArrayList<>();
            for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY)) {
                week.add(new OpeningHours(day, true, LocalTime.of(9, 0), LocalTime.of(18, 0)));
            }
            week.add(new OpeningHours(DayOfWeek.SATURDAY, true, LocalTime.of(9, 0), LocalTime.of(14, 0)));
            week.add(new OpeningHours(DayOfWeek.SUNDAY, false, null, null));
            return Optional.of(new ClinicInfo("Clínica Sonrisa", "Av. Juárez 120, Col. Centro, Puebla", "222 555 0101",
                    "contacto@sonrisa.mx", "https://sonrisa.mx/privacidad", week));
        }
    }

    static final class FakeRegistrations implements PatientRegistrationPort {
        static final String CONSENT_VERSION = "2026-09-v1";
        static final String CONSENT_TEXT = "¿Autoriza a la clínica el poder mandar recordatorios de sus citas?";

        final List<NewPatient> registered = new ArrayList<>();
        UUID lastId;

        @Override
        public ConsentText consentText() {
            return new ConsentText(CONSENT_VERSION, CONSENT_TEXT);
        }

        @Override
        public UUID register(NewPatient patient) {
            registered.add(patient);
            lastId = UUID.randomUUID();
            return lastId;
        }
    }

    static final class InMemoryDrafts implements RegistrationDraftPort {
        final Map<UUID, RegistrationDraft> byConversation = new HashMap<>();

        @Override
        public Optional<RegistrationDraft> find(UUID conversationId) {
            return Optional.ofNullable(byConversation.get(conversationId));
        }

        @Override
        public void save(RegistrationDraft draft) {
            byConversation.put(draft.conversationId(), draft);
        }

        @Override
        public void delete(UUID conversationId) {
            byConversation.remove(conversationId);
        }
    }

    @Test
    void whatThePatientTypedIsKeptBetweenMessages() {
        registerUpTo("Pérez López");
        assertFalse(drafts.byConversation.isEmpty());
    }
}
