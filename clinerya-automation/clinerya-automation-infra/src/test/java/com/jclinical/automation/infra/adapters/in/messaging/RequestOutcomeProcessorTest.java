package com.jclinical.automation.infra.adapters.in.messaging;

import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent;
import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent.Outcome;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.ports.in.HandleRequestOutcomeUseCase;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La respuesta del medico se convierte en un mensaje en cola para el paciente, una sola vez. */
class RequestOutcomeProcessorTest {

    private final HandleRequestOutcomeUseCase conversations = mock(HandleRequestOutcomeUseCase.class);
    private final OutboundMessageQueuePort outbound = mock(OutboundMessageQueuePort.class);
    private final RequestOutcomeProcessor processor = new RequestOutcomeProcessor(conversations, outbound);
    private final AppointmentRequestResolvedEvent event = new AppointmentRequestResolvedEvent(UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Outcome.BOOKED, "Dra. B",
            LocalDateTime.of(2026, 9, 29, 10, 0), LocalDateTime.of(2026, 9, 29, 10, 30), null, List.of(),
            LocalDateTime.of(2026, 9, 28, 8, 0));

    @Test
    void theReplyForThePatientIsQueued() {
        PatientNotification notification = new PatientNotification(event.clinicId(), "5215512345678",
                OutboundReply.text("Tu cita quedó confirmada"));
        when(conversations.onRequestResolved(event)).thenReturn(Optional.of(notification));

        processor.process(event);

        verify(outbound).enqueue(notification);
    }

    @Test
    void anEventTheConversationIgnoresQueuesNothing() {
        when(conversations.onRequestResolved(event)).thenReturn(Optional.empty());

        processor.process(event);

        verify(outbound, never()).enqueue(any());
    }
}
