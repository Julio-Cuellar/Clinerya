package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quien atiende un chat. {@code human}: una persona de la clinica (el agente calla). {@code since} y
 * {@code byUserId}: desde cuando y quien la activo (null: la pidio el agente). {@code replyUntil}: hasta
 * cuando WhatsApp permite escribirle texto libre (24 h desde su ultimo mensaje; null si nunca escribio).
 */
public record ChatAttention(boolean human, LocalDateTime since, UUID byUserId, LocalDateTime replyUntil) {}
