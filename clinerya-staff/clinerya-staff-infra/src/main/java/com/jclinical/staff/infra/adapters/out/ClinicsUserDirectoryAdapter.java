package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.ports.out.UserDirectoryPort;
import com.jclinical.users.domain.ports.in.GetUserProfileUseCase;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClinicsUserDirectoryAdapter implements UserDirectoryPort {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UserRepositoryPort userRepository;

    @Override
    public Optional<UserSummary> findUser(UUID userId) {
        return getUserProfileUseCase.getById(userId)
                .map(profile -> new UserSummary(profile.id(), profile.fullName(), profile.email()));
    }

    @Override
    public Optional<UserSummary> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> new UserSummary(user.getId(), user.getFullName(), user.getEmail()));
    }
}

