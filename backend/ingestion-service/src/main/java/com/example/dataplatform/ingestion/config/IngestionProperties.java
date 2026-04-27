package com.example.dataplatform.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record IngestionProperties(
        Aws aws,
        Schema schema,
        S3 s3,
        Kinesis kinesis
) {
    public record Aws(
            String region,
            String endpointOverride,
            String kinesisStreamName,
            String dataLakeBucket
    ) {
    }

    public record Schema(String path) {
    }

    public record S3(boolean enabled) {
    }

    public record Kinesis(boolean enabled) {
    }
}
