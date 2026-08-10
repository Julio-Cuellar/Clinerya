package com.jclinical.attachments.web;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        LocalDateTime createdAt
) {
}
