package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.ports.in.HandleInboundMessageUseCase;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.IntentInterpreterPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;

import java.time.Clock;
import java.util.List;

public class ConversationService implements HandleInboundMessageUseCase {

    public static final String BOOK = "action:book";
    public static final String LAST_DOCTOR = "action:last-doctor";
    public static final String SHOW_DOCTORS = "action:show-doctors";

    public ConversationService(ConversationRepositoryPort conversations, PatientDirectoryPort patients,
                               DoctorDirectoryPort doctors, SlotAvailabilityPort slots,
                               AppointmentRequestPort requests, IntentInterpreterPort interpreter, Clock clock) {
    }

    @Override
    public List<OutboundReply> handle(InboundMessage message) {
        throw new UnsupportedOperationException("pendiente");
    }
}
