package com.jclinical.patients.infra.adapters.in.web.dto;

/**
 * Decision de contacto capturada por el personal. {@code textVersion} es la version del texto que se
 * le leyo al paciente (ver GET /api/v1/patients/contact-consent-text); obligatoria al autorizar.
 */
public record ContactConsentRequest(boolean granted, String textVersion) {}
