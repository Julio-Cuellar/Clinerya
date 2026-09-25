package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensaje del paciente. {@code selectedOptionId} viene de un boton o lista (determinista);
 * {@code text} es lo que escribio y solo se interpreta si no eligio una opcion.
 */
public record InboundMessage(UUID clinicId, String fromPhone, String text, String selectedOptionId, LocalDateTime receivedAt) {}
