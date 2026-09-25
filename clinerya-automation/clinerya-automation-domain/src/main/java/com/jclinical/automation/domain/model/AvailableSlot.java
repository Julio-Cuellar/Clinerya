package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;

public record AvailableSlot(LocalDateTime start, LocalDateTime end) {}
