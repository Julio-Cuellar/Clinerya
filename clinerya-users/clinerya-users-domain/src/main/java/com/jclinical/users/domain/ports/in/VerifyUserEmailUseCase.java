package com.jclinical.users.domain.ports.in;

public interface VerifyUserEmailUseCase {

    /**
     * El codigo se busca acotado al correo. Sin acoplarlos, el token de 6 caracteres se
     * buscaba entre TODOS los pre-registros pendientes: un acierto al azar activaba la
     * cuenta de cualquiera, no la de un objetivo concreto.
     */
    boolean verifyEmail(String email, String tokenValue);
}
