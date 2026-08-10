package com.jclinical.auth.domain.ports.in;

import com.jclinical.users.domain.model.User;

public interface LoginUseCase {
    User login(String email, String password);
}
