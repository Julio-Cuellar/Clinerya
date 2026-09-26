package com.jclinical.automation.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Datos que un paciente nuevo va dando por WhatsApp antes de quedar registrado. Vive solo mientras
 * dura su conversacion: se descarta al registrarlo o al reiniciar.
 */
public record RegistrationDraft(UUID conversationId, UUID clinicId, String consentVersion, String firstName,
                                String lastNamePaterno, String lastNameMaterno, LocalDate dateOfBirth,
                                LocalDateTime updatedAt) {}
