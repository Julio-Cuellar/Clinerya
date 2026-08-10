package com.jclinical.cash.infra.adapters.in.web.dto;

import java.util.UUID;

public record VoidTicketRequest(UUID staffId, String reason) {}
