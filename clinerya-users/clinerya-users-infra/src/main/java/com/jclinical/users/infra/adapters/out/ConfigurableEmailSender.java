package com.jclinical.users.infra.adapters.out;

import com.jclinical.users.domain.ports.out.EmailSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurableEmailSender implements EmailSenderPort {

    private static final DateTimeFormatter EXPIRATION_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;

    @Value("${app.email.verification.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.from:no-reply@clinerya.local}")
    private String fromAddress;

    @Override
    public void sendVerificationCode(String email, String fullName, String code, LocalDateTime expiresAt) {
        if (!emailEnabled) {
            log.info(">>>> [VERIFICACION-DEV] Código para '{}' (válido hasta {}): {}",
                    email, expiresAt.format(EXPIRATION_FORMAT), code);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Tu código de verificación de Clinerya");
        message.setText(buildBody(fullName, code, expiresAt));

        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("No se pudo enviar el código de verificación a '{}'.", email, exception);
            throw new IllegalStateException("No se pudo enviar el correo de verificación. Intenta nuevamente.", exception);
        }
    }

    private String buildBody(String fullName, String code, LocalDateTime expiresAt) {
        return "Hola " + fullName + ",\n\n"
                + "Tu código de verificación de Clinerya es:\n\n"
                + "    " + code + "\n\n"
                + "El código vence el " + expiresAt.format(EXPIRATION_FORMAT) + ".\n"
                + "Si no solicitaste esta cuenta, puedes ignorar este mensaje.\n\n"
                + "Clinerya";
    }
}
