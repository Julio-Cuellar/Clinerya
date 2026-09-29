package com.jclinical.automation.domain.ports.out;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Datos de un numero que escribe a la clinica, tomados de su propio chat y de su perfil de WhatsApp. */
public interface ChatContactsPort {

    /** El nombre que el contacto puso en su perfil de WhatsApp (Meta lo manda con cada mensaje). */
    void saveProfileName(UUID clinicId, String phone, String profileName, LocalDateTime at);

    Map<String, String> profileNames(UUID clinicId, Collection<String> phones);

    /** Vacio si ese numero nunca escribio. */
    Optional<ChatFacts> facts(UUID clinicId, String phone);

    record ChatFacts(LocalDateTime firstMessageAt, String firstInboundText, int messageCount, String profileName) {}
}
