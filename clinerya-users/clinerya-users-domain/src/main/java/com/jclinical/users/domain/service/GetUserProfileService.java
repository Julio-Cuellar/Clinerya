package com.jclinical.users.domain.service;

import com.jclinical.users.domain.ports.in.GetUserProfileUseCase;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;

import java.util.Optional;
import java.util.UUID;

public class GetUserProfileService implements GetUserProfileUseCase {

    private final UserRepositoryPort userRepository;

    public GetUserProfileService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserProfileSnapshot> getById(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new UserProfileSnapshot(user.getId(), user.getFullName(), user.getEmail()));
    }
}
