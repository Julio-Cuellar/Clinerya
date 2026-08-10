package com.jclinical.auth.domain.ports.in;

public interface LogoutUseCase {
    void logout(String token);
}
