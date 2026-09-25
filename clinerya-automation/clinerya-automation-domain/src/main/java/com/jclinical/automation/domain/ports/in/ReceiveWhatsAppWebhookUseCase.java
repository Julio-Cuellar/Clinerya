package com.jclinical.automation.domain.ports.in;

import java.util.Optional;

/** Entrada publica del webhook de WhatsApp de cada clinica. */
public interface ReceiveWhatsAppWebhookUseCase {

    /** Verificacion de Meta al registrar el webhook: devuelve el challenge solo si el token coincide. */
    Optional<String> verifySubscription(String webhookKey, String mode, String verifyToken, String challenge);

    /** Valida la firma y acepta los mensajes; nunca procesa la conversacion aqui (se hace en segundo plano). */
    Receipt receive(String webhookKey, byte[] rawBody, String signatureHeader);

    enum Receipt {
        ACCEPTED,
        /** La clinica tiene el asistente apagado: se responde 200 a Meta pero no se procesa. */
        IGNORED,
        UNKNOWN_WEBHOOK,
        INVALID_SIGNATURE
    }
}
