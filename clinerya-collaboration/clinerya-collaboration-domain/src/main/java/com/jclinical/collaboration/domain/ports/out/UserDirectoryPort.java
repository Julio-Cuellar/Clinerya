package com.jclinical.collaboration.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface UserDirectoryPort {

    Optional<UserSummary> findByEmail(String email);

    record UserSummary(UUID id, String email, String fullName) {}
}
