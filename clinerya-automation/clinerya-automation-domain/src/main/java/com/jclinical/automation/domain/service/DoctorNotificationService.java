package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AppointmentRequest;
import com.jclinical.automation.domain.ports.in.RemindPendingRequestsUseCase;
import com.jclinical.automation.domain.ports.out.DoctorAlertPort;
import com.jclinical.automation.domain.ports.out.DoctorChannelRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorNoticeQueuePort;
import com.jclinical.automation.domain.ports.out.DoctorReplyPort;
import com.jclinical.automation.domain.ports.out.PendingRequestReminderPort;

import java.time.Clock;
import java.util.UUID;

public class DoctorNotificationService implements DoctorAlertPort, DoctorReplyPort, RemindPendingRequestsUseCase {

    public DoctorNotificationService(DoctorChannelRepositoryPort channels, DoctorNoticeQueuePort notices,
                                     PendingRequestReminderPort reminders, String inboxUrl, Clock clock) {
    }

    @Override
    public void newRequest(AppointmentRequest request) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public boolean replyIfDoctor(UUID clinicId, String fromPhone) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public int remindPending() {
        throw new UnsupportedOperationException("pendiente");
    }
}
