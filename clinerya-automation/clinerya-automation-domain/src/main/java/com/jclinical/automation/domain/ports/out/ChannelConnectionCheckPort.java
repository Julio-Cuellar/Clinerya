package com.jclinical.automation.domain.ports.out;

/** Prueba real de las credenciales contra Meta y Gemini. El detalle nunca incluye el secreto. */
public interface ChannelConnectionCheckPort {

    CheckResult checkWhatsApp(String phoneNumberId, String accessToken);

    CheckResult checkGemini(String apiKey, String model);

    record CheckResult(boolean ok, String detail) {}
}
