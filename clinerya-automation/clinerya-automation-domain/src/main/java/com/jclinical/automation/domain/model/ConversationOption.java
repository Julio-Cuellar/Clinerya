package com.jclinical.automation.domain.model;

/**
 * Opcion que el motor ofrece al paciente (boton o fila de lista). El id lleva el valor tipado
 * ("doctor:UUID", "slot:INICIO|FIN"), asi que elegirla no requiere interpretar nada mas.
 */
public record ConversationOption(String id, String label) {}
