package com.example.dataplatform.shared.db;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class JdbcOperationalRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOperationalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertProcessingError(String eventId, String stage, String errorMessage, String stack, int attempts) {
        String sql = """
                INSERT INTO processing_errors(id, event_id, stage, error_message, stack, attempts, created_at, last_attempt_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        Instant now = Instant.now();
        jdbcTemplate.update(
                sql,
                UUID.randomUUID(),
                eventId,
                stage,
                trim(errorMessage, 2000),
                trim(stack, 16000),
                attempts,
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    public void insertDlqEvent(String eventId, String payloadJson, String reason) {
        String sql = """
                INSERT INTO dlq_events(id, event_id, payload_json, reason, created_at)
                VALUES (?, ?, CAST(? AS jsonb), ?, ?)
                """;
        jdbcTemplate.update(
                sql,
                UUID.randomUUID(),
                eventId,
                payloadJson,
                trim(reason, 2000),
                Timestamp.from(Instant.now())
        );
    }

    public void insertAuditLog(String actor, String action, String detailsJson) {
        String sql = """
                INSERT INTO audit_logs(id, actor, action, details_json, created_at)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?)
                """;
        jdbcTemplate.update(
                sql,
                UUID.randomUUID(),
                actor,
                action,
                detailsJson,
                Timestamp.from(Instant.now())
        );
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
