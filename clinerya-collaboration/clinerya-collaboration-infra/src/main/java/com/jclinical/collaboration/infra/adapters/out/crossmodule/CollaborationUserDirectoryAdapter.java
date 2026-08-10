package com.jclinical.collaboration.infra.adapters.out.crossmodule;

import com.jclinical.collaboration.domain.ports.out.UserDirectoryPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CollaborationUserDirectoryAdapter implements UserDirectoryPort {

    private final UserRepositoryPort userRepository;

    @Override
    public Optional<UserSummary> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> new UserSummary(user.getId(), user.getEmail(), user.getFullName()));
    }
}
