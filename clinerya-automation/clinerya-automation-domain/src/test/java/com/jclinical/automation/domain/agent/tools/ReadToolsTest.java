package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.model.ClinicInfo.OpeningHours;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort.UpcomingVisit;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;
import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort.CatalogTreatment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Herramientas de lectura del agente. Todo sale de un puerto (caso de uso publico de otro modulo); lo
 * que el paciente puede elegir (medicos, horarios) lo arma el codigo como opciones con ids reales.
 */
class ReadToolsTest {

    /** Domingo 27/09/2026 21:00. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 27, 21, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID ramos = UUID.randomUUID();
    private final UUID diaz = UUID.randomUUID();
    private final FakeDoctors doctors = new FakeDoctors();

    private ToolContext context(PatientContact... patients) {
        return new ToolContext(clinicId, UUID.randomUUID(), "5215512345678", List.of(patients), List.of(), NOW);
    }

    // ---- info_clinica ---------------------------------------------------------------------------

    @Test
    void clinicInfoHasItsPublicDataTheGroupedScheduleAndTheFaq() {
        ClinicInfoTool tool = new ClinicInfoTool(clinic -> Optional.of(new ClinicInfo("Clínica Sonrisa", "Av. Juárez 120, Centro, Puebla",
                "222 555 0101", null, null, week())), clinic -> new AssistantProfile(null, "Aceptamos tarjeta y efectivo.", false));

        ToolOutcome outcome = tool.run(context(), Map.of());

        assertEquals("info_clinica", tool.spec().name());
        assertEquals("Clínica Sonrisa", outcome.content().get("nombre"));
        assertEquals("Av. Juárez 120, Centro, Puebla", outcome.content().get("direccion"));
        assertEquals("222 555 0101", outcome.content().get("telefono"));
        assertFalse(outcome.content().containsKey("correo"), "lo que falta no se menciona");
        assertEquals(List.of("Lunes a viernes: 09:00 a 18:00", "Sábado: 09:00 a 14:00", "Domingo: cerrado"),
                outcome.content().get("horario"));
        assertEquals("Aceptamos tarjeta y efectivo.", outcome.content().get("preguntas_frecuentes"));
        assertTrue(outcome.facts().contains("Av. Juárez 120, Centro, Puebla"), outcome.facts().toString());
    }

    @Test
    void withoutFaqThereIsNoFaqAndAnUnknownClinicIsAnError() {
        ClinicInfoTool noFaq = new ClinicInfoTool(clinic -> Optional.of(new ClinicInfo("Clínica Sonrisa", null, null, null, null,
                List.of())), clinic -> AssistantProfile.EMPTY);
        ClinicInfoTool unknown = new ClinicInfoTool(clinic -> Optional.empty(), clinic -> AssistantProfile.EMPTY);

        assertFalse(noFaq.run(context(), Map.of()).content().containsKey("preguntas_frecuentes"));
        assertTrue(unknown.run(context(), Map.of()).content().containsKey("error"));
    }

    // ---- servicios_y_precios --------------------------------------------------------------------

    private final List<CatalogTreatment> catalog = List.of(
            new CatalogTreatment("Limpieza dental", "Preventivo", new BigDecimal("650.00")),
            new CatalogTreatment("Resina", "Restaurativo", new BigDecimal("1200.50")),
            new CatalogTreatment("Valoración", null, null));

    @Test
    void servicesShowListPricesOnlyWhenTheClinicAllowsIt() {
        ServicesTool shown = new ServicesTool(clinic -> catalog, clinic -> new AssistantProfile(null, null, true));
        ServicesTool hidden = new ServicesTool(clinic -> catalog, clinic -> new AssistantProfile(null, null, false));

        ToolOutcome withPrices = shown.run(context(), Map.of());
        ToolOutcome withoutPrices = hidden.run(context(), Map.of());

        List<Map<String, Object>> services = services(withPrices);
        assertEquals(Map.of("nombre", "Limpieza dental", "categoria", "Preventivo", "precio_desde", "$650"), services.get(0));
        assertEquals("$1,200.50", services.get(1).get("precio_desde"));
        assertFalse(services.get(2).containsKey("precio_desde"), "sin precio en el catalogo no se inventa uno");
        assertTrue(withPrices.facts().contains("$650"), withPrices.facts().toString());
        assertTrue(services(withoutPrices).stream().noneMatch(service -> service.containsKey("precio_desde")));
        assertTrue(String.valueOf(withoutPrices.content().get("nota_precios")).contains("valoración"));
        assertFalse(withoutPrices.facts().contains("$650"));
    }

    @Test
    void aSearchIgnoresAccentsAndCaseAndSaysWhenNothingMatches() {
        ServicesTool tool = new ServicesTool(clinic -> catalog, clinic -> new AssistantProfile(null, null, true));

        ToolOutcome found = tool.run(context(), Map.of("busqueda", "LIMPIEZA"));
        ToolOutcome valoracion = tool.run(context(), Map.of("busqueda", "valoracion"));
        ToolOutcome missing = tool.run(context(), Map.of("busqueda", "implante"));

        assertEquals(List.of("Limpieza dental"), services(found).stream().map(s -> s.get("nombre")).toList());
        assertEquals(true, found.content().get("coincidencia"));
        assertEquals(1, services(valoracion).size());
        assertEquals(false, missing.content().get("coincidencia"));
        assertEquals(3, services(missing).size(), "sin coincidencia se muestra el catalogo para ofrecer alternativas");
    }

    // ---- medicos --------------------------------------------------------------------------------

    @Test
    void severalDoctorsAreOfferedAsAListWithTheirRealIds() {
        doctors.add(ramos, "Dra. Beatriz Ramos");
        doctors.add(diaz, "Dr. Carlos Díaz");

        ToolOutcome outcome = new DoctorsTool(doctors).run(context(), Map.of());

        assertEquals(List.of("Dra. Beatriz Ramos", "Dr. Carlos Díaz"), outcome.content().get("medicos"));
        assertEquals(List.of(new ConversationOption("doctor:" + ramos, "Dra. Beatriz Ramos"),
                new ConversationOption("doctor:" + diaz, "Dr. Carlos Díaz")), outcome.options());
    }

    @Test
    void oneDoctorNeedsNoListAndTheLastDoctorIsMentioned() {
        UUID patientId = UUID.randomUUID();
        doctors.add(ramos, "Dra. Beatriz Ramos");
        doctors.last.put(patientId, ramos);

        ToolOutcome outcome = new DoctorsTool(doctors).run(context(new PatientContact(patientId, "Ana López")), Map.of());

        assertTrue(outcome.options().isEmpty());
        assertEquals("Dra. Beatriz Ramos", outcome.content().get("ultimo_medico"));
    }

    // ---- buscar_horarios ------------------------------------------------------------------------

    @Test
    void slotsOfTheChosenDoctorComeFilteredByDaypartAsRealOptions() {
        doctors.add(ramos, "Dra. Beatriz Ramos");
        doctors.add(diaz, "Dr. Carlos Díaz");
        LocalDate thursday = LocalDate.of(2026, 10, 1);
        FakeSlots slots = new FakeSlots(List.of(slot(thursday, 10, 0), slot(thursday, 16, 0), slot(thursday, 16, 30), slot(thursday, 19, 30)));

        ToolOutcome outcome = new SlotsTool(doctors, slots)
                .run(context(), Map.of("medico", "doctor:" + ramos, "desde", "2026-10-01", "dias", 1, "turno", "tarde"));

        assertEquals(ramos, slots.lastDoctor);
        assertEquals(thursday, slots.lastFrom);
        assertEquals(List.of("slot:" + ramos + "|2026-10-01T16:00|2026-10-01T16:30", "slot:" + ramos + "|2026-10-01T16:30|2026-10-01T17:00"),
                outcome.options().stream().map(ConversationOption::id).toList());
        assertEquals("Jue 01/10 16:00", outcome.options().getFirst().label());
        assertEquals("Dra. Beatriz Ramos", outcome.content().get("medico"));
    }

    @Test
    void aDoctorThatIsNotFromTheClinicIsRejectedWithoutSearching() {
        doctors.add(ramos, "Dra. Beatriz Ramos");
        FakeSlots slots = new FakeSlots(List.of());

        ToolOutcome outcome = new SlotsTool(doctors, slots).run(context(), Map.of("medico", "doctor:" + UUID.randomUUID()));

        assertTrue(outcome.content().containsKey("error"));
        assertEquals(null, slots.lastDoctor);
    }

    @Test
    void withOneDoctorItIsUsedAndWithSeveralItAsksWhichOne() {
        doctors.add(ramos, "Dra. Beatriz Ramos");
        FakeSlots slots = new FakeSlots(List.of(slot(NOW.toLocalDate().plusDays(1), 9, 0)));

        new SlotsTool(doctors, slots).run(context(), Map.of());
        assertEquals(ramos, slots.lastDoctor);

        doctors.add(diaz, "Dr. Carlos Díaz");
        ToolOutcome ask = new SlotsTool(doctors, new FakeSlots(List.of())).run(context(), Map.of());
        assertTrue(ask.content().containsKey("error"));
        assertEquals(2, ask.options().size(), "se ofrece elegir medico");
    }

    @Test
    void pastOrInvalidDatesStartTodayAndTheRangeIsBounded() {
        doctors.add(ramos, "Dra. Beatriz Ramos");
        FakeSlots slots = new FakeSlots(List.of());

        new SlotsTool(doctors, slots).run(context(), Map.of("desde", "2020-01-01", "dias", 90));
        assertEquals(NOW.toLocalDate(), slots.lastFrom);
        assertEquals(14, slots.lastDays);

        ToolOutcome none = new SlotsTool(doctors, slots).run(context(), Map.of("desde", "el jueves"));
        assertEquals(NOW.toLocalDate(), slots.lastFrom);
        assertEquals(true, none.content().get("sin_horarios"));
        assertTrue(none.options().isEmpty());
    }

    // ---- mis_citas ------------------------------------------------------------------------------

    @Test
    void myAppointmentsNeedARegisteredPatient() {
        ToolOutcome outcome = new MyAppointmentsTool((clinic, patient, limit) -> List.of(), doctors).run(context(), Map.of());

        assertEquals(false, outcome.content().get("paciente_registrado"));
    }

    @Test
    void myAppointmentsListTheUpcomingOnesWithTheDoctorsName() {
        doctors.add(ramos, "Dra. Beatriz Ramos");
        UUID ana = UUID.randomUUID();
        LocalDateTime monday = LocalDateTime.of(2026, 9, 28, 10, 0);
        PatientAppointmentsPort appointments = (clinic, patient, limit) -> patient.equals(ana)
                ? List.of(new UpcomingVisit(UUID.randomUUID(), ramos, monday, monday.plusMinutes(30), true)) : List.of();

        ToolOutcome outcome = new MyAppointmentsTool(appointments, doctors).run(context(new PatientContact(ana, "Ana López")), Map.of());

        List<Map<String, Object>> citas = listOfMaps(outcome.content().get("citas"));
        assertEquals(Map.of("paciente", "Ana López", "fecha", "Lun 28/09 10:00", "medico", "Dra. Beatriz Ramos", "confirmada", true),
                citas.getFirst());
        assertTrue(outcome.facts().contains("Lun 28/09 10:00"));
    }

    // ---- utilidades -----------------------------------------------------------------------------

    private static List<OpeningHours> week() {
        List<OpeningHours> week = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SUNDAY) week.add(new OpeningHours(day, false, null, null));
            else if (day == DayOfWeek.SATURDAY) week.add(new OpeningHours(day, true, LocalTime.of(9, 0), LocalTime.of(14, 0)));
            else week.add(new OpeningHours(day, true, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        }
        return week;
    }

    private static AvailableSlot slot(LocalDate date, int hour, int minute) {
        LocalDateTime start = date.atTime(hour, minute);
        return new AvailableSlot(start, start.plusMinutes(30));
    }

    private static List<Map<String, Object>> services(ToolOutcome outcome) {
        return listOfMaps(outcome.content().get("servicios"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listOfMaps(Object value) {
        return (List<Map<String, Object>>) value;
    }

    static final class FakeDoctors implements DoctorDirectoryPort {
        final List<DoctorContact> all = new ArrayList<>();
        final Map<UUID, UUID> last = new HashMap<>();

        void add(UUID id, String name) {
            all.add(new DoctorContact(id, name));
        }

        @Override public List<DoctorContact> listDoctors(UUID clinicId) { return List.copyOf(all); }

        @Override
        public Optional<DoctorContact> lastDoctorOf(UUID clinicId, UUID patientId) {
            UUID id = last.get(patientId);
            return all.stream().filter(doctor -> doctor.staffId().equals(id)).findFirst();
        }
    }

    static final class FakeSlots implements SlotAvailabilityPort {
        private final List<AvailableSlot> slots;
        UUID lastDoctor;
        LocalDate lastFrom;
        int lastDays;

        FakeSlots(List<AvailableSlot> slots) {
            this.slots = slots;
        }

        @Override
        public List<AvailableSlot> availableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit) {
            lastDoctor = doctorStaffId;
            lastFrom = from;
            lastDays = days;
            return slots.stream().limit(limit).toList();
        }
    }
}
