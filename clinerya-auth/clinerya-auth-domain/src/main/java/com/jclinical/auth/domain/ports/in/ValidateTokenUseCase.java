package com.jclinical.auth.domain.ports.in;

public interface ValidateTokenUseCase {
    boolean validate(String token);
}
