package com.example.dataplatform.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record IngestionProperties(
        Aws aws,
        Schema schema
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
}
