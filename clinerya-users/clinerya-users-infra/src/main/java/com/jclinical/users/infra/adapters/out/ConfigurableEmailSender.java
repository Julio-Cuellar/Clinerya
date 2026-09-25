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

    @Override
    public void sendStaffInvitation(String email, String role, String invitationUrl, LocalDateTime expiresAt) {
        if (!emailEnabled) {
            // Sin correo (desarrollo) la liga solo existe aqui: sin ella no se puede probar el alta del invitado.
            log.info(">>>> [INVITACION-DEV] Liga para '{}' como {} (válida hasta {}): {}",
                    email, role, expiresAt.format(EXPIRATION_FORMAT), invitationUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Invitacion para unirte a Clinerya");
        message.setText(buildStaffInvitationBody(role, invitationUrl, expiresAt));

        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("No se pudo enviar la invitacion de personal a '{}'.", email, exception);
            throw new IllegalStateException("No se pudo enviar la invitacion por correo. Intenta nuevamente.", exception);
        }
    }

    @Override
    public void sendPasswordReset(String email, String fullName, String resetUrl, LocalDateTime expiresAt) {
        if (!emailEnabled) {
            log.info(">>>> [RECUPERACION-DEV] Liga para '{}' (válida hasta {}): {}",
                    email, expiresAt.format(EXPIRATION_FORMAT), resetUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Restablece tu contrasena de Clinerya");
        message.setText(buildPasswordResetBody(fullName, resetUrl, expiresAt));

        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("No se pudo enviar la recuperacion de contrasena a '{}'.", email, exception);
            throw new IllegalStateException("No se pudo enviar el correo de recuperación. Intenta nuevamente.", exception);
        }
    }

    private String buildStaffInvitationBody(String role, String invitationUrl, LocalDateTime expiresAt) {
        return "Has sido invitado a unirte a una clinica en Clinerya con el rol de " + role + ".\n\n"
                + "Acepta tu invitacion aqui:\n"
                + invitationUrl + "\n\n"
                + "El enlace vence el " + expiresAt.format(EXPIRATION_FORMAT) + ".\n"
                + "Si no esperabas esta invitacion, puedes ignorar este correo.\n\n"
                + "Clinerya";
    }

    private String buildPasswordResetBody(String fullName, String resetUrl, LocalDateTime expiresAt) {
        return "Hola " + fullName + ",\n\n"
                + "Solicitaste restablecer tu contrasena de Clinerya.\n\n"
                + "Crea una nueva contrasena aqui:\n"
                + resetUrl + "\n\n"
                + "El enlace vence el " + expiresAt.format(EXPIRATION_FORMAT) + ".\n"
                + "Si no solicitaste este cambio, puedes ignorar este correo.\n\n"
                + "Clinerya";
    }

    @Override
    public void sendShareRecipientVerificationCode(String email, String code, LocalDateTime expiresAt) {
        if (!emailEnabled) {
            log.info(">>>> [VERIFICACION-ENLACE-DEV] Código para '{}' (válido hasta {}): {}",
                    email, expiresAt.format(EXPIRATION_FORMAT), code);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Código para consultar tu expediente compartido");
        message.setText(buildShareVerificationBody(code, expiresAt));

        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("No se pudo enviar el código de verificación de enlace compartido a '{}'.", email, exception);
            throw new IllegalStateException("No se pudo enviar el código de verificación. Intenta nuevamente.", exception);
        }
    }

    private String buildShareVerificationBody(String code, LocalDateTime expiresAt) {
        return "Hola,\n\n"
                + "Para consultar el expediente clínico que compartieron contigo, ingresa este código:\n\n"
                + "    " + code + "\n\n"
                + "El código vence el " + expiresAt.format(EXPIRATION_FORMAT) + ".\n"
                + "Si no esperabas esta consulta, puedes ignorar este correo.\n\n"
                + "Clinerya";
    }
}
