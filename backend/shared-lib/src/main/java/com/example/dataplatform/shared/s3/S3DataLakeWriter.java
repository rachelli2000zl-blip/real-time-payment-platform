package com.example.dataplatform.shared.s3;

import com.example.dataplatform.shared.model.PaymentEvent;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class S3DataLakeWriter {

    private final S3Client s3Client;
    private final String bucketName;
    private final Clock clock;

    public S3DataLakeWriter(S3Client s3Client, String bucketName) {
        this(s3Client, bucketName, Clock.systemUTC());
    }

    public S3DataLakeWriter(S3Client s3Client, String bucketName, Clock clock) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.clock = clock;
    }

    public void writeRaw(PaymentEvent event, String payloadJson) {
        String key = buildKey("raw", event);
        put(key, payloadJson);
    }

    public void writeProcessed(PaymentEvent event, String payloadJson) {
        String key = buildKey("processed", event);
        put(key, payloadJson);
    }

    private String buildKey(String zone, PaymentEvent event) {
        String dt = event.occurredAt() != null
                ? event.occurredAt().atOffset(ZoneOffset.UTC).toLocalDate().toString()
                : LocalDate.now(clock).toString();

        return "%s/event_type=%s/dt=%s/%s.json".formatted(
                zone,
                event.eventType(),
                dt,
                event.eventId()
        );
    }

    private void put(String key, String payloadJson) {
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("app.aws.dataLakeBucket must be configured");
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/json")
                .build();

        s3Client.putObject(request, RequestBody.fromString(payloadJson, StandardCharsets.UTF_8));
    }
}
