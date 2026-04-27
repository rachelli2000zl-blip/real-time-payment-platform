package com.example.dataplatform.shared.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcProcessedEventRepositoryTest {

    private JdbcProcessedEventRepository repository;

    @BeforeEach
    void setup() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS processed_events (
                    event_id TEXT PRIMARY KEY,
                    processed_at TIMESTAMP,
                    partition_key TEXT,
                    shard_id TEXT,
                    sequence_number TEXT
                )
                """);

        repository = new JdbcProcessedEventRepository(jdbcTemplate);
    }

    @Test
    void insertsOnlyOnceByEventId() {
        boolean first = repository.insertIfAbsent(
                "evt-1",
                Instant.now(),
                "cust-1",
                "shard-1",
                "1"
        );

        boolean second = repository.insertIfAbsent(
                "evt-1",
                Instant.now(),
                "cust-1",
                "shard-1",
                "2"
        );

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }
}
