package com.example.dataplatform.ingestion.service;

import com.example.dataplatform.ingestion.config.IngestionProperties;
import com.example.dataplatform.shared.model.PaymentEvent;
import com.example.dataplatform.shared.s3.S3DataLakeWriter;
import com.example.dataplatform.shared.schema.PaymentSchemaValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;

import java.time.Instant;

@Service
public class IngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestionService.class);

    private final PaymentSchemaValidator schemaValidator;
    private final ObjectMapper objectMapper;
    private final S3DataLakeWriter dataLakeWriter;
    private final KinesisClient kinesisClient;
    private final IngestionProperties properties;

    public IngestionService(
            PaymentSchemaValidator schemaValidator,
            ObjectMapper objectMapper,
            S3DataLakeWriter dataLakeWriter,
            KinesisClient kinesisClient,
            IngestionProperties properties
    ) {
        this.schemaValidator = schemaValidator;
        this.objectMapper = objectMapper;
        this.dataLakeWriter = dataLakeWriter;
        this.kinesisClient = kinesisClient;
        this.properties = properties;
    }

    public void ingest(String payloadJson, String requestId) {
        schemaValidator.validate(payloadJson);

        try {
            ObjectNode enriched = (ObjectNode) objectMapper.readTree(payloadJson);
            enriched.put("requestId", requestId);
            enriched.put("receivedAt", Instant.now().toString());

            String enrichedJson = objectMapper.writeValueAsString(enriched);
            PaymentEvent event = objectMapper.treeToValue(enriched, PaymentEvent.class);

            if (isS3WriteEnabled()) {
                dataLakeWriter.writeRaw(event, enrichedJson);
            } else {
                LOGGER.info("Skipping S3 raw write because app.s3.enabled=false requestId={} eventId={}",
                        requestId,
                        event.eventId());
            }

            if (isKinesisPublishEnabled()) {
                PutRecordRequest putRecordRequest = PutRecordRequest.builder()
                        .streamName(properties.aws().kinesisStreamName())
                        .partitionKey(event.customerId())
                        .data(SdkBytes.fromUtf8String(enrichedJson))
                        .build();
                kinesisClient.putRecord(putRecordRequest);
            } else {
                LOGGER.info("Skipping Kinesis publish because app.kinesis.enabled=false requestId={} eventId={}",
                        requestId,
                        event.eventId());
            }

            LOGGER.info("event_ingested requestId={} eventId={} eventType={} customerId={}",
                    requestId,
                    event.eventId(),
                    event.eventType(),
                    event.customerId());
        } catch (Exception exception) {
            LOGGER.error("Failed to ingest event", exception);
            throw new IllegalStateException("Failed to ingest event", exception);
        }
    }

    private boolean isS3WriteEnabled() {
        return properties.s3() != null && properties.s3().enabled();
    }

    private boolean isKinesisPublishEnabled() {
        return properties.kinesis() != null && properties.kinesis().enabled();
    }
}
