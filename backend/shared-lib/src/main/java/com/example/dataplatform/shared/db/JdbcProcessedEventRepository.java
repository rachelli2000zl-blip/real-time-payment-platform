package com.example.dataplatform.shared.db;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

public class JdbcProcessedEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProcessedEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean insertIfAbsent(
            String eventId,
            Instant processedAt,
            String partitionKey,
            String shardId,
            String sequenceNumber
    ) {
        String sql = """
                INSERT INTO processed_events(event_id, processed_at, partition_key, shard_id, sequence_number)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """;

        int rows = jdbcTemplate.update(sql,
                eventId,
                Timestamp.from(processedAt),
                partitionKey,
                shardId,
                sequenceNumber);
        return rows > 0;
    }
}
