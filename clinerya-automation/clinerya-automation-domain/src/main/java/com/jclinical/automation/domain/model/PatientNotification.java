package com.jclinical.automation.domain.model;

import java.util.UUID;

/** Mensaje que la automatizacion le debe enviar al paciente por iniciativa propia. */
public record PatientNotification(UUID clinicId, String phone, OutboundReply reply) {}
