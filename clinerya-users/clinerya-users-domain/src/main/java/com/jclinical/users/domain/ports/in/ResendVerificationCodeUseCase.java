package com.jclinical.users.domain.ports.in;

public interface ResendVerificationCodeUseCase {

    boolean resendVerificationCode(String email);
}
