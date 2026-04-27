package com.example.dataplatform.control.dto;

import java.time.Instant;
import java.util.UUID;

public record DlqEventView(
        UUID id,
        String eventId,
        String payloadJson,
        String reason,
        Instant createdAt
) {
}
