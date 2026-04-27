package com.example.dataplatform.control.service;

import com.example.dataplatform.control.config.ControlPlaneProperties;
import com.example.dataplatform.control.dto.MetricPoint;
import com.example.dataplatform.control.dto.SummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class SummaryService {

    private final JdbcTemplate jdbcTemplate;
    private final SqsClient sqsClient;
    private final ControlPlaneProperties properties;

    public SummaryService(JdbcTemplate jdbcTemplate, SqsClient sqsClient, ControlPlaneProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqsClient = sqsClient;
        this.properties = properties;
    }

    public SummaryResponse getSummary() {
        Long processed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE processed_at >= now() - interval '15 minutes'",
                Long.class
        );
        Long errors = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processing_errors WHERE created_at >= now() - interval '15 minutes'",
                Long.class
        );

        Timestamp latestProcessed = jdbcTemplate.queryForObject(
                "SELECT MAX(processed_at) FROM processed_events",
                Timestamp.class
        );

        Instant latestProcessedInstant = latestProcessed == null ? null : latestProcessed.toInstant();
        double throughputPerMinute = (processed == null ? 0 : processed) / 15.0;
        double errorRate = processed == null || processed == 0
                ? 0.0
                : (errors == null ? 0 : errors) / (double) processed;

        long lagSeconds = latestProcessedInstant == null
                ? -1
                : Math.max(0, Duration.between(latestProcessedInstant, Instant.now()).toSeconds());

        long dlqDepth = getDlqDepth();
        List<MetricPoint> series = loadSeries();

        return new SummaryResponse(
                throughputPerMinute,
                errorRate,
                dlqDepth,
                lagSeconds,
                latestProcessedInstant,
                series
        );
    }

    private List<MetricPoint> loadSeries() {
        String sql = """
                SELECT bucket,
                       SUM(processed_count) AS processed,
                       SUM(error_count) AS errors
                FROM (
                    SELECT date_trunc('minute', processed_at) AS bucket,
                           COUNT(*) AS processed_count,
                           0 AS error_count
                    FROM processed_events
                    WHERE processed_at >= now() - interval '15 minutes'
                    GROUP BY 1
                    UNION ALL
                    SELECT date_trunc('minute', created_at) AS bucket,
                           0 AS processed_count,
                           COUNT(*) AS error_count
                    FROM processing_errors
                    WHERE created_at >= now() - interval '15 minutes'
                    GROUP BY 1
                ) combined
                GROUP BY 1
                ORDER BY 1
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new MetricPoint(
                rs.getTimestamp("bucket").toInstant().toString(),
                rs.getLong("processed"),
                rs.getLong("errors")
        ));
    }

    private long getDlqDepth() {
        String dlqQueueUrl = properties.getAws().getDlqQueueUrl();
        if (dlqQueueUrl == null || dlqQueueUrl.isBlank()) {
            return 0;
        }

        Map<QueueAttributeName, String> attributes = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(dlqQueueUrl)
                        .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                        .build())
                .attributes();

        return Long.parseLong(attributes.getOrDefault(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES, "0"));
    }
}
