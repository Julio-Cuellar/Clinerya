package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.model.BookableSlot;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.automation.domain.model.AvailableSlot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** La automatizacion ofrece exactamente los cupos que calcula la agenda, sin reglas propias. */
class SlotAvailabilityAdapterTest {

    @Test
    void offersTheSlotsTheAgendaComputes() {
        OnlineBookingUseCase agenda = mock(OnlineBookingUseCase.class);
        UUID clinicId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 9, 28);
        LocalDateTime start = LocalDateTime.of(2026, 9, 28, 10, 0);
        when(agenda.findAvailableSlots(clinicId, doctorId, from, 14, 10))
                .thenReturn(List.of(new BookableSlot(start, start.plusMinutes(30))));

        List<AvailableSlot> slots = new SlotAvailabilityAdapter(agenda).availableSlots(clinicId, doctorId, from, 14, 10);

        assertEquals(List.of(new AvailableSlot(start, start.plusMinutes(30))), slots);
    }
}
