package com.jclinical.users.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreRegistration {
    private UUID id;
    private String email;
    private String fullName;
    private String passwordHash;
    private String clinicName;
    private String verificationToken;
    private LocalDateTime verificationTokenExpiresAt;
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return verificationTokenExpiresAt != null && LocalDateTime.now().isAfter(verificationTokenExpiresAt);
    }
}
