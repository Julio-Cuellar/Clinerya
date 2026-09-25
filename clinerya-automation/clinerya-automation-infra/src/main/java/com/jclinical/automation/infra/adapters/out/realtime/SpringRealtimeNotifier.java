package com.jclinical.automation.infra.adapters.out.realtime;

import com.jclinical.automation.domain.ports.out.RealtimeNotifierPort;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.UUID;

public class SpringRealtimeNotifier implements RealtimeNotifierPort {

    public SpringRealtimeNotifier(ApplicationEventPublisher events) {
    }

    @Override
    public void chatActivity(UUID clinicId, String phone, LocalDateTime at) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public void newAppointmentRequest(UUID clinicId, UUID doctorStaffId, UUID requestId) {
        throw new UnsupportedOperationException("pendiente");
    }
}
