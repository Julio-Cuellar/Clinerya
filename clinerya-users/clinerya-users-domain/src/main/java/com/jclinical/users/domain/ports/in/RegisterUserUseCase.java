package com.jclinical.users.domain.ports.in;

import com.jclinical.users.domain.model.User;

public interface RegisterUserUseCase {
    User registerUser(RegisterUserCommand command);

    record RegisterUserCommand(
        String email,
        String password,
        String fullName,
        String clinicName
    ) {
        public RegisterUserCommand {
            if (email == null || email.trim().isBlank()) {
                throw new IllegalArgumentException("El correo electrónico es obligatorio");
            }
            if (password == null || password.trim().isBlank() || password.length() < 8) {
                throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
            }
            if (fullName == null || fullName.trim().isBlank()) {
                throw new IllegalArgumentException("El nombre completo es obligatorio");
            }
            if (clinicName == null || clinicName.trim().isBlank()) {
                throw new IllegalArgumentException("El nombre de la clínica es obligatorio");
            }
            
            // Normalizar el correo
            email = email.trim().toLowerCase();
        }
    }
}
