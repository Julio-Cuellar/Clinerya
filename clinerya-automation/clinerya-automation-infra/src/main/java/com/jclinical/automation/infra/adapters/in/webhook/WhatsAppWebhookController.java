package com.jclinical.automation.infra.adapters.in.webhook;

import com.jclinical.automation.domain.ports.in.ReceiveWhatsAppWebhookUseCase;
import com.jclinical.automation.domain.ports.in.ReceiveWhatsAppWebhookUseCase.Receipt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook publico de WhatsApp de cada clinica (lo llama Meta, sin JWT). La llave de la URL elige la
 * clinica y la firma HMAC con el secreto de su app prueba el origen. Responde en cuanto registra:
 * la conversacion se procesa en segundo plano.
 */
@RestController
@RequestMapping("/api/v1/public/whatsapp/{webhookKey}")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final ReceiveWhatsAppWebhookUseCase webhook;
    private final WebhookRateLimiter rateLimiter;

    @GetMapping
    public ResponseEntity<String> verify(@PathVariable String webhookKey,
                                         @RequestParam(name = "hub.mode", required = false) String mode,
                                         @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
                                         @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if (!rateLimiter.tryAcquire(webhookKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        return webhook.verifySubscription(webhookKey, mode, verifyToken, challenge)
                .map(echo -> ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(echo))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PostMapping
    public ResponseEntity<Void> receive(@PathVariable String webhookKey,
                                        @RequestBody(required = false) byte[] body,
                                        @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {
        if (!rateLimiter.tryAcquire(webhookKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        Receipt receipt = webhook.receive(webhookKey, body == null ? new byte[0] : body, signature);
        return switch (receipt) {
            case ACCEPTED, IGNORED -> ResponseEntity.ok().build();
            case UNKNOWN_WEBHOOK -> ResponseEntity.notFound().build();
            case INVALID_SIGNATURE -> {
                log.warn(">>>> [AUTOMATIZACION] Webhook de WhatsApp con firma invalida rechazado");
                yield ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        };
    }
}
