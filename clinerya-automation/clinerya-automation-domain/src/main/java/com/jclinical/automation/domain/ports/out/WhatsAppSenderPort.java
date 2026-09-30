package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.OutboundReply;

import java.util.List;

/** Envio por la API de WhatsApp Cloud con las credenciales de la clinica. */
public interface WhatsAppSenderPort {

    /** Texto con sus opciones (botones o lista). Solo dentro de la ventana de 24 h. */
    SendResult sendMessage(Credentials credentials, String to, OutboundReply reply);

    /** Plantilla aprobada por Meta: la unica forma de escribir fuera de la ventana de 24 h. */
    SendResult sendTemplate(Credentials credentials, String to, String templateName, String languageCode,
                            List<String> parameters);

    /**
     * Plantilla propia con botones de respuesta rapida: cada boton devuelve su payload cuando el paciente
     * lo toca. La plantilla aprobada en Meta debe tener esos botones en ese orden.
     */
    default SendResult sendTemplate(Credentials credentials, String to, String templateName, String languageCode,
                                    List<String> parameters, List<String> buttonPayloads) {
        return sendTemplate(credentials, to, templateName, languageCode, parameters);
    }

    record Credentials(String phoneNumberId, String accessToken) {}

    record SendResult(boolean sent, String waMessageId, boolean retryable, String error) {

        public static SendResult ok(String waMessageId) {
            return new SendResult(true, waMessageId, false, null);
        }

        public static SendResult failed(boolean retryable, String error) {
            return new SendResult(false, null, retryable, error);
        }
    }
}
