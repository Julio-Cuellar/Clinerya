package com.jclinical.core.security;

public class ClinicAccessDeniedException extends RuntimeException {

    public ClinicAccessDeniedException(String message) {
        super(message);
    }
}
