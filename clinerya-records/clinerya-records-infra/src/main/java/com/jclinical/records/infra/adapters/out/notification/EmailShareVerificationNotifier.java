package com.jclinical.records.infra.adapters.out.notification;

import com.jclinical.records.domain.ports.out.ShareVerificationNotifierPort;
import com.jclinical.users.domain.ports.out.EmailSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EmailShareVerificationNotifier implements ShareVerificationNotifierPort {

    private final EmailSenderPort emailSender;

    @Override
    public void sendVerificationCode(String email, String code, LocalDateTime expiresAt) {
        emailSender.sendShareRecipientVerificationCode(email, code, expiresAt);
    }
}
