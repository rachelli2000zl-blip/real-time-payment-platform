package com.example.dataplatform.control.service;

import com.example.dataplatform.control.config.ControlPlaneProperties;
import com.example.dataplatform.control.dto.DlqEventView;
import com.example.dataplatform.control.dto.DlqReplayRequest;
import com.example.dataplatform.shared.config.JacksonSupport;
import com.example.dataplatform.shared.db.JdbcOperationalRepository;
import com.example.dataplatform.shared.model.RetryMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DlqService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SqsClient sqsClient;
    private final ControlPlaneProperties properties;
    private final JdbcOperationalRepository operationalRepository;
    private final ObjectMapper objectMapper = JacksonSupport.objectMapper();

    public DlqService(
            NamedParameterJdbcTemplate jdbcTemplate,
            SqsClient sqsClient,
            ControlPlaneProperties properties,
            JdbcOperationalRepository operationalRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.operationalRepository = operationalRepository;
    }

    public List<DlqEventView> listDlqEvents(int limit) {
        return jdbcTemplate.getJdbcTemplate().query(
                """
                        SELECT id, event_id, payload_json::text AS payload_json, reason, created_at
                        FROM dlq_events
                        ORDER BY created_at DESC
                        LIMIT ?
                        """,
                ps -> ps.setInt(1, Math.min(Math.max(limit, 1), 500)),
                (rs, rowNum) -> new DlqEventView(
                        rs.getObject("id", UUID.class),
                        rs.getString("event_id"),
                        rs.getString("payload_json"),
                        rs.getString("reason"),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
    }

    public int replay(DlqReplayRequest request) {
        if (request.ids() == null || request.ids().isEmpty()) {
            return 0;
        }

        List<UUID> ids = request.ids().stream().map(UUID::fromString).toList();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT id, event_id, payload_json::text AS payload_json
                        FROM dlq_events
                        WHERE id IN (:ids)
                        """,
                params
        );

        String retryQueue = properties.getAws().getRetryQueueUrl();
        int count = 0;

        for (Map<String, Object> row : rows) {
            String eventId = (String) row.get("event_id");
            String payload = (String) row.get("payload_json");
            RetryMessage message = new RetryMessage(eventId, payload, 1, "manual replay", Instant.now());
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(retryQueue)
                    .messageBody(toJson(message))
                    .build());
            count++;
        }

        operationalRepository.insertAuditLog(
                request.actor() == null || request.actor().isBlank() ? "unknown" : request.actor(),
                "dlq.replay",
                toJson(Map.of(
                        "ids", ids.stream().map(UUID::toString).collect(Collectors.toList()),
                        "count", count
                ))
        );

        return count;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize value", exception);
        }
    }
}
