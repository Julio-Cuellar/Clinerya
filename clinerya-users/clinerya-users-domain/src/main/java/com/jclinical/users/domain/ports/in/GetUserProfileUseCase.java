package com.jclinical.users.domain.ports.in;

import java.util.Optional;
import java.util.UUID;

public interface GetUserProfileUseCase {

    Optional<UserProfileSnapshot> getById(UUID userId);

    record UserProfileSnapshot(
            UUID id,
            String fullName,
            String email
    ) {}
}
