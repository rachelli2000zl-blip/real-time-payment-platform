package com.example.dataplatform.control.service;

import com.example.dataplatform.control.dto.ProcessingErrorView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ErrorService {

    private final JdbcTemplate jdbcTemplate;

    public ErrorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProcessingErrorView> listErrors(String eventId, String stage, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, event_id, stage, error_message, stack, attempts, created_at, last_attempt_at
                FROM processing_errors
                WHERE 1=1
                """);

        List<Object> args = new ArrayList<>();
        if (eventId != null && !eventId.isBlank()) {
            sql.append(" AND event_id = ?");
            args.add(eventId);
        }
        if (stage != null && !stage.isBlank()) {
            sql.append(" AND stage = ?");
            args.add(stage);
        }

        sql.append(" ORDER BY created_at DESC LIMIT ?");
        args.add(Math.min(Math.max(limit, 1), 500));

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> new ProcessingErrorView(
                rs.getObject("id", java.util.UUID.class),
                rs.getString("event_id"),
                rs.getString("stage"),
                rs.getString("error_message"),
                rs.getString("stack"),
                rs.getInt("attempts"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_attempt_at").toInstant()
        ));
    }
}
