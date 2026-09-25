package com.jclinical.patients.domain.model;

/**
 * Texto oficial que se le lee al paciente antes de marcar la casilla de contacto. Vive en el backend
 * con una version para que el registro pruebe exactamente que autorizo: si el texto cambia, se sube
 * la version y un alta hecha con el texto anterior se rechaza.
 */
public final class ContactConsentText {

    public static final String CURRENT_VERSION = "2026-09-v1";

    public static final String CURRENT_TEXT = "¿Autoriza a la clínica el poder mandar recordatorios de sus citas, "
            + "seguimiento de tratamientos, o cualquier evento que sea necesario de su consulta por medio de los "
            + "canales de contacto proporcionados?";

    private ContactConsentText() {
    }
}
