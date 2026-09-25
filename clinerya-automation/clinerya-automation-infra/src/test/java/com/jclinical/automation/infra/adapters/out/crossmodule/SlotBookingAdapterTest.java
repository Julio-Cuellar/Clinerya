package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.model.SlotHold;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.BookHeldSlotCommand;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.HoldSlotCommand;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.SlotUnavailableException;
import com.jclinical.agenda.domain.service.OnlineBookingService;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort.SlotNoLongerAvailableException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La automatizacion aparta y agenda solo a traves de la agenda. Usa el servicio de dominio (no el
 * envoltorio transaccional): corre dentro de la transaccion de la automatizacion, y una negativa de
 * la agenda no debe marcar esa transaccion para rollback, porque la conversacion la maneja.
 */
class SlotBookingAdapterTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 29, 10, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final OnlineBookingService agenda = mock(OnlineBookingService.class);
    private final SlotBookingAdapter adapter = new SlotBookingAdapter(agenda);

    @Test
    void holdingAsksTheAgendaForTheExactSlot() {
        UUID reference = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        when(agenda.holdSlot(new HoldSlotCommand(clinicId, doctorId, START, START.plusMinutes(30), reference, 1440)))
                .thenReturn(new SlotHold(holdId, clinicId, doctorId, START, START.plusMinutes(30), START, reference,
                        SlotHold.Status.ACTIVE, START));

        assertEquals(holdId, adapter.hold(clinicId, doctorId, START, START.plusMinutes(30), reference, 1440));
    }

    @Test
    void aSlotTheAgendaRefusesBecomesSlotNoLongerAvailable() {
        when(agenda.holdSlot(any())).thenThrow(new SlotUnavailableException("El horario elegido ya no está disponible."));
        when(agenda.bookHeldSlot(any())).thenThrow(new SlotUnavailableException("El doctor ya tiene otra cita agendada en ese horario."));

        assertThrows(SlotNoLongerAvailableException.class,
                () -> adapter.hold(clinicId, doctorId, START, START.plusMinutes(30), UUID.randomUUID(), 60));
        SlotNoLongerAvailableException error = assertThrows(SlotNoLongerAvailableException.class,
                () -> adapter.book(clinicId, UUID.randomUUID(), UUID.randomUUID(), "motivo"));
        assertEquals("El doctor ya tiene otra cita agendada en ese horario.", error.getMessage());
    }

    @Test
    void bookingReleasingAndDoctorLookupGoThroughTheAgenda() {
        UUID holdId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(agenda.bookHeldSlot(new BookHeldSlotCommand(clinicId, holdId, patientId, "Solicitada por WhatsApp")))
                .thenReturn(appointmentId);
        when(agenda.doctorStaffIdOfUser(clinicId, userId)).thenReturn(Optional.of(doctorId));

        assertEquals(appointmentId, adapter.book(clinicId, holdId, patientId, "Solicitada por WhatsApp"));
        adapter.release(clinicId, holdId);
        verify(agenda).releaseHold(clinicId, holdId);
        assertEquals(Optional.of(doctorId), adapter.doctorStaffIdOfUser(clinicId, userId));
    }
}
