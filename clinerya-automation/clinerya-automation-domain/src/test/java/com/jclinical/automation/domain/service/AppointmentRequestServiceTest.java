package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AppointmentRequest;
import com.jclinical.automation.domain.model.AppointmentRequest.Status;
import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent;
import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent.Outcome;
import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort.NewAppointmentRequest;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort.SlotNoLongerAvailableException;
import com.jclinical.automation.domain.ports.out.AppointmentRequestRepositoryPort;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;
import com.jclinical.automation.domain.ports.out.SlotBookingPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import com.jclinical.core.security.ClinicAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entrega 4 (CU-3): la solicitud del paciente llega al medico asignado, que la acepta, la rechaza o
 * propone otros horarios. Solo ese medico puede responder; la cita se crea unicamente al aprobarla
 * (o al elegir el paciente una opcion que el medico ya aprobo al proponerla). Sin respuesta en 24 h
 * la solicitud vence y el cupo se libera.
 */
class AppointmentRequestServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 28, 8, 0);
    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 29, 10, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID doctorUserId = UUID.randomUUID();
    private final UUID otherDoctorId = UUID.randomUUID();
    private final UUID otherDoctorUserId = UUID.randomUUID();

    private final InMemoryRequests requests = new InMemoryRequests();
    private final FakeBooking booking = new FakeBooking();
    private final FakeSlots slots = new FakeSlots();
    private final List<Published> events = new ArrayList<>();
    private final List<AppointmentRequest> alerted = new ArrayList<>();

    private AppointmentRequestService service;

    @BeforeEach
    void setUp() {
        booking.doctorOfUser.put(doctorUserId, doctorId);
        booking.doctorOfUser.put(otherDoctorUserId, otherDoctorId);
        service = serviceAt(NOW);
    }

    // ---- envio del paciente -----------------------------------------------------------------

    @Test
    void submittingHoldsTheSlotForTheDoctorsResponseWindow() {
        UUID requestId = service.submit(newRequest(doctorId, START));

        AppointmentRequest saved = requests.byId.get(requestId);
        assertEquals(Status.PENDING, saved.status());
        assertEquals("Ana López", saved.patientName());
        assertEquals("Dra. B", saved.doctorName());
        FakeBooking.Held held = booking.active.get(saved.holdId());
        assertEquals(requestId, held.reference(), "el apartado apunta a la solicitud");
        assertEquals(24 * 60, held.minutes());
        assertEquals(START, held.start());
        assertTrue(events.isEmpty(), "enviar no notifica a la conversacion");
    }

    @Test
    void submittingAlertsTheDoctor() {
        UUID requestId = service.submit(newRequest(doctorId, START));

        assertEquals(List.of(requests.byId.get(requestId)), alerted);
    }

    @Test
    void aSlotTakenMeanwhileIsNotSubmitted() {
        booking.taken.add(START);

        assertThrows(SlotNoLongerAvailableException.class, () -> service.submit(newRequest(doctorId, START)));
        assertTrue(requests.byId.isEmpty());
        assertTrue(alerted.isEmpty(), "sin solicitud no hay aviso");
    }

    // ---- bandeja del medico -----------------------------------------------------------------

    @Test
    void theDoctorSeesOnlyTheirOwnPendingRequests() {
        UUID mine = service.submit(newRequest(doctorId, START));
        UUID answered = service.submit(newRequest(doctorId, START.plusHours(1)));
        service.submit(newRequest(otherDoctorId, START));
        service.reject(doctorUserId, clinicId, answered, null);

        List<AppointmentRequest> pending = service.listPending(doctorUserId, clinicId);

        assertEquals(List.of(mine), pending.stream().map(AppointmentRequest::id).toList());
        assertTrue(service.listPending(UUID.randomUUID(), clinicId).isEmpty(), "quien no es medico no ve solicitudes");
    }

    @Test
    void onlyTheAssignedDoctorCanRespond() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        UUID stranger = UUID.randomUUID();

        assertThrows(ClinicAccessDeniedException.class, () -> service.accept(otherDoctorUserId, clinicId, requestId));
        assertThrows(ClinicAccessDeniedException.class, () -> service.reject(otherDoctorUserId, clinicId, requestId, null));
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.proposeOptions(otherDoctorUserId, clinicId, requestId, List.of(slot(START.plusDays(1)))));
        assertThrows(ClinicAccessDeniedException.class, () -> service.proposableSlots(otherDoctorUserId, clinicId, requestId));
        assertThrows(ClinicAccessDeniedException.class, () -> service.accept(stranger, clinicId, requestId));
        assertEquals(Status.PENDING, requests.byId.get(requestId).status());
    }

    @Test
    void aRequestFromAnotherClinicDoesNotExist() {
        UUID requestId = service.submit(newRequest(doctorId, START));

        assertThrows(IllegalArgumentException.class, () -> service.accept(doctorUserId, UUID.randomUUID(), requestId));
    }

    // ---- aceptar ----------------------------------------------------------------------------

    @Test
    void acceptingBooksTheHeldSlotAndNotifiesTheConversation() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        UUID holdId = requests.byId.get(requestId).holdId();

        AppointmentRequest accepted = service.accept(doctorUserId, clinicId, requestId);

        assertEquals(Status.BOOKED, accepted.status());
        assertEquals(booking.appointmentId, accepted.appointmentId());
        assertEquals(List.of(holdId), booking.booked);
        assertEquals(patientId, booking.lastPatient);
        AppointmentRequestResolvedEvent event = singleEvent();
        assertEquals(Outcome.BOOKED, event.outcome());
        assertEquals(conversationId, event.conversationId());
        assertEquals(requestId, event.requestId());
        assertEquals(START, event.start());
        assertEquals("Dra. B", event.doctorName());
    }

    @Test
    void whenTheAgendaRefusesTheBookingTheRequestStaysPending() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        booking.rejectBookingWith = "El doctor ya tiene otra cita agendada en ese horario.";

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.accept(doctorUserId, clinicId, requestId));

        assertEquals("El doctor ya tiene otra cita agendada en ese horario.", error.getMessage());
        assertEquals(Status.PENDING, requests.byId.get(requestId).status());
        assertTrue(events.isEmpty());
    }

    // ---- rechazar ---------------------------------------------------------------------------

    @Test
    void rejectingReleasesTheSlotAndTellsThePatientWhy() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        UUID holdId = requests.byId.get(requestId).holdId();

        AppointmentRequest rejected = service.reject(doctorUserId, clinicId, requestId, "  Estaré en un congreso  ");

        assertEquals(Status.REJECTED, rejected.status());
        assertEquals("Estaré en un congreso", rejected.rejectionReason());
        assertEquals(List.of(holdId), booking.released);
        AppointmentRequestResolvedEvent event = singleEvent();
        assertEquals(Outcome.REJECTED, event.outcome());
        assertEquals("Estaré en un congreso", event.reason());
    }

    @Test
    void aBlankRejectionReasonIsStoredAsNone() {
        UUID requestId = service.submit(newRequest(doctorId, START));

        assertNull(service.reject(doctorUserId, clinicId, requestId, "   ").rejectionReason());
    }

    @Test
    void aRequestIsAnsweredOnlyOnce() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        service.reject(doctorUserId, clinicId, requestId, null);

        assertThrows(IllegalStateException.class, () -> service.accept(doctorUserId, clinicId, requestId));
        assertThrows(IllegalStateException.class, () -> service.reject(doctorUserId, clinicId, requestId, null));
        assertTrue(booking.booked.isEmpty());
    }

    // ---- proponer opciones ------------------------------------------------------------------

    @Test
    void theDoctorCanProposeFromTheirFreeSlots() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        slots.byDoctor.put(doctorId, List.of(slot(START.plusDays(1)), slot(START.plusDays(2))));

        assertEquals(List.of(slot(START.plusDays(1)), slot(START.plusDays(2))),
                service.proposableSlots(doctorUserId, clinicId, requestId));
    }

    @Test
    void proposingHoldsEachOptionAndReleasesTheOriginalSlot() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        UUID originalHold = requests.byId.get(requestId).holdId();
        List<AvailableSlot> options = List.of(slot(START.plusDays(1)), slot(START.plusDays(2)));

        AppointmentRequest proposed = service.proposeOptions(doctorUserId, clinicId, requestId, options);

        assertEquals(Status.OPTIONS_PROPOSED, proposed.status());
        assertEquals(2, proposed.proposedOptions().size());
        for (AppointmentRequest.ProposedOption option : proposed.proposedOptions()) {
            FakeBooking.Held held = booking.active.get(option.holdId());
            assertEquals(requestId, held.reference());
            assertEquals(24 * 60, held.minutes());
            assertEquals(option.start(), held.start());
        }
        assertEquals(List.of(originalHold), booking.released);
        AppointmentRequestResolvedEvent event = singleEvent();
        assertEquals(Outcome.OPTIONS_PROPOSED, event.outcome());
        assertEquals(options, event.options());
    }

    @Test
    void proposalsAreOneToThreeDistinctSlotsOtherThanTheRequestedOne() {
        UUID requestId = service.submit(newRequest(doctorId, START));

        assertThrows(IllegalArgumentException.class,
                () -> service.proposeOptions(doctorUserId, clinicId, requestId, List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.proposeOptions(doctorUserId, clinicId, requestId,
                List.of(slot(START.plusDays(1)), slot(START.plusDays(2)), slot(START.plusDays(3)), slot(START.plusDays(4)))));
        assertThrows(IllegalArgumentException.class, () -> service.proposeOptions(doctorUserId, clinicId, requestId,
                List.of(slot(START.plusDays(1)), slot(START.plusDays(1)))));
        assertThrows(IllegalArgumentException.class, () -> service.proposeOptions(doctorUserId, clinicId, requestId,
                List.of(slot(START))), "el horario pedido se acepta, no se propone");
        assertEquals(Status.PENDING, requests.byId.get(requestId).status());
        assertTrue(booking.released.isEmpty());
    }

    @Test
    void ifAProposedSlotIsTakenNothingIsProposed() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        booking.taken.add(START.plusDays(2));

        assertThrows(IllegalStateException.class, () -> service.proposeOptions(doctorUserId, clinicId, requestId,
                List.of(slot(START.plusDays(1)), slot(START.plusDays(2)))));

        assertEquals(Status.PENDING, requests.byId.get(requestId).status());
        assertEquals(1, booking.active.size(), "solo queda el apartado original");
        assertTrue(booking.active.containsKey(requests.byId.get(requestId).holdId()));
    }

    // ---- eleccion del paciente --------------------------------------------------------------

    @Test
    void choosingAProposedOptionBooksItDirectlyAndReleasesTheOthers() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        AppointmentRequest proposed = service.proposeOptions(doctorUserId, clinicId, requestId,
                List.of(slot(START.plusDays(1)), slot(START.plusDays(2))));
        AppointmentRequest.ProposedOption first = proposed.proposedOptions().get(0);
        AppointmentRequest.ProposedOption second = proposed.proposedOptions().get(1);

        UUID appointmentId = service.chooseOption(clinicId, requestId, second.start(), second.end());

        assertEquals(booking.appointmentId, appointmentId);
        assertEquals(List.of(second.holdId()), booking.booked);
        assertTrue(booking.released.contains(first.holdId()));
        AppointmentRequest booked = requests.byId.get(requestId);
        assertEquals(Status.BOOKED, booked.status());
        assertEquals(appointmentId, booked.appointmentId());
        assertEquals(1, events.size(), "solo el evento de la propuesta: la eleccion responde en la misma conversacion");
    }

    @Test
    void onlyAProposedOptionCanBeChosen() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        service.proposeOptions(doctorUserId, clinicId, requestId, List.of(slot(START.plusDays(1))));

        assertThrows(IllegalArgumentException.class,
                () -> service.chooseOption(clinicId, requestId, START.plusDays(5), START.plusDays(5).plusMinutes(30)));
        assertTrue(booking.booked.isEmpty());
    }

    @Test
    void ifTheChosenOptionCanNoLongerBeBookedThePatientCanChooseAgain() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        AppointmentRequest proposed = service.proposeOptions(doctorUserId, clinicId, requestId,
                List.of(slot(START.plusDays(1))));
        booking.rejectBookingWith = "El doctor ya tiene otra cita agendada en ese horario.";
        AppointmentRequest.ProposedOption option = proposed.proposedOptions().get(0);

        assertThrows(SlotNoLongerAvailableException.class,
                () -> service.chooseOption(clinicId, requestId, option.start(), option.end()));
        assertEquals(Status.OPTIONS_PROPOSED, requests.byId.get(requestId).status());
    }

    @Test
    void decliningTheProposalReleasesEveryOption() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        AppointmentRequest proposed = service.proposeOptions(doctorUserId, clinicId, requestId,
                List.of(slot(START.plusDays(1)), slot(START.plusDays(2))));

        service.declineOptions(clinicId, requestId);

        assertEquals(Status.DECLINED, requests.byId.get(requestId).status());
        proposed.proposedOptions().forEach(option -> assertTrue(booking.released.contains(option.holdId())));
        assertTrue(booking.active.isEmpty());
    }

    // ---- vencimiento ------------------------------------------------------------------------

    @Test
    void aRequestWithoutAnswerExpiresAfter24HoursAndReleasesTheSlot() {
        UUID overdue = service.submit(newRequest(doctorId, START));
        UUID recent = serviceAt(NOW.plusHours(2)).submit(newRequest(doctorId, START.plusHours(1)));
        UUID holdId = requests.byId.get(overdue).holdId();

        int expired = serviceAt(NOW.plusHours(24)).expireOverdue();

        assertEquals(1, expired);
        assertEquals(Status.EXPIRED, requests.byId.get(overdue).status());
        assertEquals(Status.PENDING, requests.byId.get(recent).status());
        assertEquals(List.of(holdId), booking.released);
        assertEquals(Outcome.EXPIRED, singleEvent().outcome());
    }

    @Test
    void proposedOptionsExpire24HoursAfterTheProposal() {
        UUID requestId = service.submit(newRequest(doctorId, START));
        AppointmentRequest proposed = serviceAt(NOW.plusHours(1)).proposeOptions(doctorUserId, clinicId, requestId,
                List.of(slot(START.plusDays(1))));
        events.clear();

        assertEquals(0, serviceAt(NOW.plusHours(24).plusMinutes(30)).expireOverdue(), "el paciente tiene 24 h desde la propuesta");
        assertEquals(1, serviceAt(NOW.plusHours(25)).expireOverdue());

        assertEquals(Status.EXPIRED, requests.byId.get(requestId).status());
        assertTrue(booking.released.contains(proposed.proposedOptions().get(0).holdId()));
        assertEquals(Outcome.EXPIRED, singleEvent().outcome());
    }

    @Test
    void anOverdueRequestCanNoLongerBeAnswered() {
        UUID requestId = service.submit(newRequest(doctorId, START));

        assertThrows(IllegalStateException.class,
                () -> serviceAt(NOW.plusHours(24)).accept(doctorUserId, clinicId, requestId));
        assertTrue(booking.booked.isEmpty());
    }

    // ---- utilidades -------------------------------------------------------------------------

    private AppointmentRequestService serviceAt(LocalDateTime now) {
        return new AppointmentRequestService(requests, booking, slots,
                (routingKey, payload) -> events.add(new Published(routingKey, payload)), alerted::add,
                Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private NewAppointmentRequest newRequest(UUID doctor, LocalDateTime start) {
        return new NewAppointmentRequest(clinicId, conversationId, patientId, doctor, start, start.plusMinutes(30),
                "5215512345678", "Ana López", doctor.equals(doctorId) ? "Dra. B" : "Dr. C");
    }

    private static AvailableSlot slot(LocalDateTime start) {
        return new AvailableSlot(start, start.plusMinutes(30));
    }

    private AppointmentRequestResolvedEvent singleEvent() {
        assertEquals(1, events.size());
        assertEquals(DomainEventRoutingKeys.APPOINTMENT_REQUEST_RESOLVED, events.get(0).routingKey());
        return (AppointmentRequestResolvedEvent) events.get(0).payload();
    }

    record Published(String routingKey, Object payload) {}

    static final class InMemoryRequests implements AppointmentRequestRepositoryPort {
        final Map<UUID, AppointmentRequest> byId = new LinkedHashMap<>();

        @Override
        public AppointmentRequest save(AppointmentRequest request) {
            byId.put(request.id(), request);
            return request;
        }

        @Override
        public Optional<AppointmentRequest> findByIdAndClinicId(UUID requestId, UUID clinicId) {
            return Optional.ofNullable(byId.get(requestId)).filter(request -> request.clinicId().equals(clinicId));
        }

        @Override
        public List<AppointmentRequest> findPendingByDoctor(UUID clinicId, UUID doctorStaffId) {
            return byId.values().stream()
                    .filter(request -> request.clinicId().equals(clinicId) && request.doctorStaffId().equals(doctorStaffId))
                    .filter(request -> request.status() == Status.PENDING)
                    .toList();
        }

        @Override
        public List<AppointmentRequest> findOverdue(LocalDateTime cutoff) {
            return byId.values().stream()
                    .filter(request -> request.status() == Status.PENDING || request.status() == Status.OPTIONS_PROPOSED)
                    .toList();
        }
    }

    static final class FakeBooking implements SlotBookingPort {
        record Held(UUID clinicId, UUID doctorStaffId, LocalDateTime start, LocalDateTime end, UUID reference, int minutes) {}

        final Map<UUID, Held> active = new LinkedHashMap<>();
        final List<UUID> released = new ArrayList<>();
        final List<UUID> booked = new ArrayList<>();
        final Set<LocalDateTime> taken = new HashSet<>();
        final Map<UUID, UUID> doctorOfUser = new HashMap<>();
        final UUID appointmentId = UUID.randomUUID();
        UUID lastPatient;
        String rejectBookingWith;

        @Override
        public UUID hold(UUID clinicId, UUID doctorStaffId, LocalDateTime start, LocalDateTime end, UUID reference, int holdMinutes) {
            boolean alreadyHeld = active.values().stream()
                    .anyMatch(held -> held.doctorStaffId().equals(doctorStaffId) && held.start().equals(start));
            if (taken.contains(start) || alreadyHeld) {
                throw new SlotNoLongerAvailableException();
            }
            UUID holdId = UUID.randomUUID();
            active.put(holdId, new Held(clinicId, doctorStaffId, start, end, reference, holdMinutes));
            return holdId;
        }

        @Override
        public void release(UUID clinicId, UUID holdId) {
            active.remove(holdId);
            released.add(holdId);
        }

        @Override
        public UUID book(UUID clinicId, UUID holdId, UUID patientId, String reason) {
            if (rejectBookingWith != null) {
                throw new SlotNoLongerAvailableException(rejectBookingWith);
            }
            if (active.remove(holdId) == null) {
                throw new SlotNoLongerAvailableException();
            }
            booked.add(holdId);
            lastPatient = patientId;
            return appointmentId;
        }

        @Override
        public Optional<UUID> doctorStaffIdOfUser(UUID clinicId, UUID userId) {
            return Optional.ofNullable(doctorOfUser.get(userId));
        }
    }

    static final class FakeSlots implements SlotAvailabilityPort {
        final Map<UUID, List<AvailableSlot>> byDoctor = new HashMap<>();

        @Override
        public List<AvailableSlot> availableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit) {
            return byDoctor.getOrDefault(doctorStaffId, List.of()).stream().limit(limit).toList();
        }
    }
}
