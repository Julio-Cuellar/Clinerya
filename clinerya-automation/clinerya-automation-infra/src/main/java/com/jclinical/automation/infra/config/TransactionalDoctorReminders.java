package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.ports.in.RemindPendingRequestsUseCase;
import com.jclinical.automation.domain.service.DoctorNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Encolar el recordatorio y marcar la solicitud como recordada van juntos: no se repite ni se pierde. */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalDoctorReminders implements RemindPendingRequestsUseCase {

    private final DoctorNotificationService notifications;

    @Override
    @Transactional
    public int remindPending() {
        return notifications.remindPending();
    }
}
