package com.jclinical.agenda.infra.adapters.in.web.dto;

import com.jclinical.agenda.domain.model.RoomBlockType;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateRoomBlockRequest(
        UUID roomId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        RoomBlockType type,
        String reason
) {}
