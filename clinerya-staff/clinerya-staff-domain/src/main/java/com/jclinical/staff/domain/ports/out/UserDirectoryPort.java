package com.jclinical.staff.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface UserDirectoryPort {

    Optional<UserSummary> findUser(UUID userId);

    Optional<UserSummary> findByEmail(String email);

    record UserSummary(
            UUID id,
            String fullName,
            String email
    ) {}
}

