package com.jclinical.auth.domain.ports.in;

public interface RefreshTokenUseCase {
    String refresh(String refreshToken);
}
