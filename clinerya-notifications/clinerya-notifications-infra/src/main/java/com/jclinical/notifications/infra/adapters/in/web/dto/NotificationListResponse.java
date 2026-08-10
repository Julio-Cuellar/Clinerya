package com.jclinical.notifications.infra.adapters.in.web.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> items,
        long unreadCount
) {}
