package com.example.dataplatform.shared.model;

import java.time.Instant;

public record RetryMessage(
        String eventId,
        String payloadJson,
        int attempts,
        String lastError,
        Instant nextAttemptAt
) {
}
