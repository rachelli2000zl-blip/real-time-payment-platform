package com.example.dataplatform.control.dto;

import java.time.Instant;
import java.util.UUID;

public record ProcessingErrorView(
        UUID id,
        String eventId,
        String stage,
        String errorMessage,
        String stack,
        int attempts,
        Instant createdAt,
        Instant lastAttemptAt
) {
}
