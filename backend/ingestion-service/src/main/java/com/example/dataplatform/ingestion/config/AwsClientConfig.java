package com.example.dataplatform.ingestion.config;

import com.example.dataplatform.shared.config.JacksonSupport;
import com.example.dataplatform.shared.s3.S3DataLakeWriter;
import com.example.dataplatform.shared.schema.PaymentSchemaValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
public class AwsClientConfig {

    @Bean
    public S3Client s3Client(IngestionProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.aws().region()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (properties.aws().endpointOverride() != null && !properties.aws().endpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.aws().endpointOverride()));
        }

        return builder.build();
    }

    @Bean
    public KinesisClient kinesisClient(IngestionProperties properties) {
        KinesisClientBuilder builder = KinesisClient.builder()
                .region(Region.of(properties.aws().region()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (properties.aws().endpointOverride() != null && !properties.aws().endpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.aws().endpointOverride()));
        }

        return builder.build();
    }

    @Bean
    public PaymentSchemaValidator paymentSchemaValidator(IngestionProperties properties) {
        String path = properties.schema() != null ? properties.schema().path() : "shared/schemas/payment/v1.json";
        return new PaymentSchemaValidator(path);
    }

    @Bean
    public S3DataLakeWriter s3DataLakeWriter(S3Client s3Client, IngestionProperties properties) {
        return new S3DataLakeWriter(s3Client, properties.aws().dataLakeBucket());
    }

    @Bean
    public ObjectMapper objectMapper() {
        return JacksonSupport.objectMapper();
    }
}
