package com.jclinical.records.domain.ports.out;

import java.time.LocalDateTime;

/** Envia el codigo de un solo uso para verificar al destinatario de un enlace compartido. */
public interface ShareVerificationNotifierPort {

    void sendVerificationCode(String email, String code, LocalDateTime expiresAt);
}
