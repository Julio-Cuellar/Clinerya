package com.jclinical.users.domain.ports.in;

public interface VerifyUserEmailUseCase {
    boolean verifyEmail(String tokenValue);
}
