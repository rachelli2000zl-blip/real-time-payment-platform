package com.example.dataplatform.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String customerId,
        BigDecimal amount,
        String currency,
        Integer schemaVersion,
        Map<String, Object> metadata,
        String requestId,
        Instant receivedAt
) {
}
