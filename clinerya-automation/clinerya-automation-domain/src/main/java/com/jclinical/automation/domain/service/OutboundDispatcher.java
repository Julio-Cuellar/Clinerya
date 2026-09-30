package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.QueuedMessage;
import com.jclinical.automation.domain.model.QueuedMessage.Audience;
import com.jclinical.automation.domain.ports.in.DispatchOutboundMessagesUseCase;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.automation.domain.ports.out.CustomerServiceWindowPort;
import com.jclinical.automation.domain.ports.out.OutboundDispatchRepositoryPort;
import com.jclinical.automation.domain.ports.out.WhatsAppSenderPort;
import com.jclinical.automation.domain.ports.out.WhatsAppSenderPort.Credentials;
import com.jclinical.automation.domain.ports.out.WhatsAppSenderPort.SendResult;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Envia la cola de salida con las credenciales de cada clinica. Dentro de la ventana de 24 h desde
 * el ultimo mensaje de ese celular va texto libre con sus opciones; fuera de ella WhatsApp solo
 * acepta una plantilla aprobada (D7). Fallas temporales se reintentan con espera creciente.
 */
public class OutboundDispatcher implements DispatchOutboundMessagesUseCase {

    static final int MAX_ATTEMPTS = 5;
    static final Duration CUSTOMER_SERVICE_WINDOW = Duration.ofHours(24);
    /** Espera antes del intento 2, 3, 4 y 5. */
    static final List<Duration> BACKOFF = List.of(
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(60));

    private final OutboundDispatchRepositoryPort queue;
    private final ChannelSettingsRepositoryPort settings;
    private final CustomerServiceWindowPort window;
    private final WhatsAppSenderPort sender;
    private final Clock clock;

    public OutboundDispatcher(OutboundDispatchRepositoryPort queue, ChannelSettingsRepositoryPort settings,
                              CustomerServiceWindowPort window, WhatsAppSenderPort sender, Clock clock) {
        this.queue = queue;
        this.settings = settings;
        this.window = window;
        this.sender = sender;
        this.clock = clock;
    }

    @Override
    public boolean dispatchNext() {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<QueuedMessage> next = queue.lockNextDue(now);
        if (next.isEmpty()) {
            return false;
        }
        QueuedMessage message = next.get();
        Optional<ChannelSettings> clinic = settings.findByClinicId(message.clinicId()).filter(OutboundDispatcher::canSend);
        if (clinic.isEmpty()) {
            queue.markFailed(message.id(), message.attempts() + 1,
                    "El asistente de WhatsApp de la clínica está desactivado o sin credenciales.", now);
            return true;
        }
        Credentials credentials = new Credentials(clinic.get().whatsappPhoneNumberId(), clinic.get().whatsappAccessToken());

        if (windowIsOpen(message, now)) {
            record(message, sender.sendMessage(credentials, message.phone(), message.reply()), false, now);
            return true;
        }
        boolean ownTemplate = message.templateName() != null && !message.templateName().isBlank();
        String template = ownTemplate ? message.templateName()
                : message.audience() == Audience.DOCTOR ? clinic.get().doctorTemplateName() : clinic.get().patientTemplateName();
        if (template == null || template.isBlank()) {
            queue.markFailed(message.id(), message.attempts() + 1,
                    "Pasaron más de 24 h desde el último mensaje de ese número y la clínica no tiene una plantilla aprobada configurada.",
                    now);
            return true;
        }
        SendResult result = ownTemplate
                ? sender.sendTemplate(credentials, message.phone(), template, clinic.get().templateLanguage(),
                        message.templateParameters(), message.reply().options().stream().map(ConversationOption::id).toList())
                : sender.sendTemplate(credentials, message.phone(), template, clinic.get().templateLanguage(),
                        message.templateParameters());
        record(message, result, true, now);
        return true;
    }

    private void record(QueuedMessage message, SendResult result, boolean viaTemplate, LocalDateTime now) {
        if (result.sent()) {
            queue.markSent(message.id(), result.waMessageId(), viaTemplate, now);
            return;
        }
        int attempts = message.attempts() + 1;
        if (!result.retryable() || attempts >= MAX_ATTEMPTS) {
            queue.markFailed(message.id(), attempts, result.error(), now);
            return;
        }
        queue.scheduleRetry(message.id(), attempts, now.plus(BACKOFF.get(attempts - 1)), result.error());
    }

    private boolean windowIsOpen(QueuedMessage message, LocalDateTime now) {
        return window.lastInboundAt(message.clinicId(), message.phone())
                .map(lastInbound -> lastInbound.isAfter(now.minus(CUSTOMER_SERVICE_WINDOW)))
                .orElse(false);
    }

    private static boolean canSend(ChannelSettings clinic) {
        return clinic.enabled() && clinic.whatsappPhoneNumberId() != null && clinic.whatsappAccessToken() != null;
    }
}
