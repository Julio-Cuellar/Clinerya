package com.jclinical.users.domain.ports.out;

public interface PasswordHasherPort {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String securedPassword);
}
