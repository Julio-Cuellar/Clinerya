package com.jclinical.users.domain.model.events;

import java.util.UUID;

public final class UserEvents {
    private UserEvents() {
        // Clase de utilidad contenedora
    }

    public record UserRegisteredEvent(
        UUID userId,
        String email,
        String fullName,
        String clinicName,
        String verificationToken
    ) {}

    public record UserEmailVerifiedEvent(
        UUID userId,
        String email,
        String fullName,
        String clinicName
    ) {}
}
