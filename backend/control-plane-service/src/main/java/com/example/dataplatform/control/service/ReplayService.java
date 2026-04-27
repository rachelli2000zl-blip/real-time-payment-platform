package com.example.dataplatform.control.service;

import com.example.dataplatform.control.config.ControlPlaneProperties;
import com.example.dataplatform.shared.config.JacksonSupport;
import com.example.dataplatform.shared.db.JdbcOperationalRepository;
import com.example.dataplatform.shared.model.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReplayService {

    private final S3Client s3Client;
    private final KinesisClient kinesisClient;
    private final JdbcOperationalRepository operationalRepository;
    private final ControlPlaneProperties properties;
    private final ObjectMapper objectMapper = JacksonSupport.objectMapper();

    public ReplayService(
            S3Client s3Client,
            KinesisClient kinesisClient,
            JdbcOperationalRepository operationalRepository,
            ControlPlaneProperties properties
    ) {
        this.s3Client = s3Client;
        this.kinesisClient = kinesisClient;
        this.operationalRepository = operationalRepository;
        this.properties = properties;
    }

    public int replayByDate(LocalDate date, String eventType, int requestedMaxRecords, String actor) {
        int maxRecords = Math.min(requestedMaxRecords, properties.getReplay().getMaxRecords());
        String prefix = "raw/event_type=%s/dt=%s/".formatted(eventType, date);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(properties.getAws().getDataLakeBucket())
                .prefix(prefix)
                .maxKeys(maxRecords)
                .build();

        int count = 0;
        for (S3Object object : s3Client.listObjectsV2Paginator(request).contents()) {
            if (count >= maxRecords) {
                break;
            }

            String payload = readPayload(object.key());
            PaymentEvent event = parseEvent(payload);
            putToKinesis(payload, event.customerId());
            count++;
        }

        Map<String, Object> details = new HashMap<>();
        details.put("date", date.toString());
        details.put("eventType", eventType);
        details.put("count", count);
        details.put("maxRecords", maxRecords);

        operationalRepository.insertAuditLog(
                actor == null || actor.isBlank() ? "unknown" : actor,
                "replay.by-date",
                toJson(details)
        );

        return count;
    }

    private PaymentEvent parseEvent(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentEvent.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse event during replay", exception);
        }
    }

    private String readPayload(String key) {
        try (ResponseInputStream<?> stream = s3Client.getObject(GetObjectRequest.builder()
                .bucket(properties.getAws().getDataLakeBucket())
                .key(key)
                .build())) {
            return new String(stream.readAllBytes());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read object: " + key, exception);
        }
    }

    private void putToKinesis(String payload, String partitionKey) {
        kinesisClient.putRecord(PutRecordRequest.builder()
                .streamName(properties.getAws().getKinesisStreamName())
                .partitionKey(partitionKey)
                .data(SdkBytes.fromUtf8String(payload))
                .build());
    }

    private String toJson(Object details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize audit details", exception);
        }
    }
}
