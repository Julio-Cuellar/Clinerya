package com.jclinical.integrations.infra.adapters.in.web.dto;

public record CalendarConnectionStatusResponse(boolean connected, String email, boolean importPastEvents) {}

