package com.jclinical.agenda.domain.model;

import java.time.LocalDateTime;

/** Cupo libre que se puede ofrecer a un paciente para una cita en linea. */
public record BookableSlot(LocalDateTime start, LocalDateTime end) {}
