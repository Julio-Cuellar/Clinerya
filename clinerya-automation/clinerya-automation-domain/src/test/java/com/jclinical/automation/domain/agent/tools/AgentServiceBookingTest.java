package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.tools.BookingToolsTest.FakeRequests;
import com.jclinical.automation.domain.agent.tools.BookingToolsTest.InMemoryConversations;
import com.jclinical.automation.domain.agent.tools.BookingToolsTest.InMemoryPending;
import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;
import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort.CatalogTreatment;
import com.jclinical.automation.domain.agent.tools.ReadToolsTest.FakeDoctors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan v2, S3: el agente explica el servicio con su descripcion, da el precio segun su tipo (fijo:
 * "cuesta"; variable: "desde"), busca horarios donde cabe la duracion del servicio y la solicitud al
 * medico lleva el servicio para que la cita copie su precio.
 */
class AgentServiceBookingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 28, 8, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID ramos = UUID.randomUUID();
    private final UUID cleaning = UUID.randomUUID();
    private final UUID braces = UUID.randomUUID();
    private final List<CatalogTreatment> catalog = List.of(
            new CatalogTreatment(cleaning, "Limpieza dental", "Preventiva",
                    "Retiramos sarro y placa con ultrasonido y pulimos tus dientes.", true, new BigDecimal("650"), 45),
            new CatalogTreatment(braces, "Ortodoncia", "Ortodoncia",
                    "Brackets para alinear tus dientes; el plan se define en la valoración.", false, new BigDecimal("18000"), 60));
    private final FakeDoctors doctors = new FakeDoctors();
    private final RecordingSlots slots = new RecordingSlots();
    private final InMemoryPending pending = new InMemoryPending();
    private final FakeRequests requests = new FakeRequests();
    private final InMemoryConversations conversations = new InMemoryConversations();
    private final PatientContact ana = new PatientContact(UUID.randomUUID(), "Ana López");
    private final Conversation conversation = new Conversation(UUID.randomUUID(), clinicId, "5215512345678",
            ConversationState.CONVERSANDO, null, null, null, null, null, List.of(), 0, NOW.minusMinutes(5), NOW.minusMinutes(5));

    @BeforeEach
    void setUp() {
        doctors.add(ramos, "Dra. Beatriz Ramos");
        conversations.save(conversation);
    }

    @Test
    void aFixedPriceServiceIsExplainedAndSaysWhatItCosts() {
        ToolOutcome outcome = services().run(context(NOW, List.of()), Map.of("busqueda", "limpieza"));

        Map<String, Object> service = firstService(outcome);
        assertEquals("$650", service.get("precio"));
        assertFalse(service.containsKey("precio_desde"), "un precio fijo no es un precio desde");
        assertEquals("Retiramos sarro y placa con ultrasonido y pulimos tus dientes.", service.get("descripcion"));
        assertEquals(45, service.get("duracion_minutos"));
    }

    @Test
    void aVariablePriceServiceSaysFromAndThatTheDoctorDefinesIt() {
        ToolOutcome outcome = services().run(context(NOW, List.of()), Map.of("busqueda", "ortodoncia"));

        Map<String, Object> service = firstService(outcome);
        assertEquals("$18,000", service.get("precio_desde"));
        assertFalse(service.containsKey("precio"));
        assertEquals(true, service.get("precio_varia"));
    }

    @Test
    void servicesOfferedToChooseCarryTheirIds() {
        ToolOutcome outcome = services().run(context(NOW, List.of()), Map.of("busqueda", "dental ortodoncia"));

        assertTrue(outcome.options().stream().map(ConversationOption::id).toList()
                .containsAll(List.of(ServicesTool.OPTION_PREFIX + cleaning, ServicesTool.OPTION_PREFIX + braces)));
    }

    @Test
    void slotsForAServiceLastAsLongAsTheServiceAndRememberIt() {
        ToolOutcome outcome = slotsTool().run(context(NOW, List.of()),
                Map.of("medico", "doctor:" + ramos, "servicio", ServicesTool.OPTION_PREFIX + cleaning));

        assertEquals(45, slots.lastDuration);
        ConversationOption first = outcome.options().getFirst();
        SlotsTool.ChosenSlot chosen = SlotsTool.parse(first.id());
        assertEquals(cleaning, chosen.serviceId());
        assertEquals(chosen.start().plusMinutes(45), chosen.end());
    }

    @Test
    void theBookingRequestCarriesTheChosenService() {
        LocalDateTime thursday = LocalDateTime.of(2026, 10, 1, 16, 0);
        ConversationOption slot = new ConversationOption("slot:" + ramos + "|" + thursday + "|" + thursday.plusMinutes(45)
                + "|" + cleaning, "Jue 01/10 16:00");

        new ProposeBookingTool(doctors, pending).run(context(NOW, List.of(slot), ana), Map.of("horario", slot.id()));
        PendingAction proposed = pending.find(conversation.id()).orElseThrow();
        assertEquals(cleaning, proposed.serviceId());

        new ConfirmActionTool(pending, requests, conversations, (clinic, appointment, patient, reason) -> { }, request -> { })
                .run(context(NOW.plusMinutes(1), List.of(), ana), Map.of());
        assertEquals(cleaning, requests.submitted.getFirst().serviceId());
    }

    private ServicesTool services() {
        return new ServicesTool(clinic -> catalog, clinic -> new AssistantProfile(null, null, true));
    }

    private SlotsTool slotsTool() {
        return new SlotsTool(doctors, slots, clinic -> catalog);
    }

    private ToolContext context(LocalDateTime now, List<ConversationOption> offered, PatientContact... patients) {
        return new ToolContext(clinicId, conversation.id(), conversation.phone(), List.of(patients), offered, now);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstService(ToolOutcome outcome) {
        return ((List<Map<String, Object>>) outcome.content().get("servicios")).getFirst();
    }

    static final class RecordingSlots implements SlotAvailabilityPort {
        Integer lastDuration;

        @Override
        public List<AvailableSlot> availableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit) {
            return availableSlots(clinicId, doctorStaffId, from, days, limit, null);
        }

        @Override
        public List<AvailableSlot> availableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit,
                                                  Integer durationMinutes) {
            lastDuration = durationMinutes;
            int length = durationMinutes == null ? 30 : durationMinutes;
            List<AvailableSlot> found = new ArrayList<>();
            LocalDateTime start = from.plusDays(1).atTime(9, 0);
            found.add(new AvailableSlot(start, start.plusMinutes(length)));
            return found;
        }
    }
}
