package com.example.dataplatform.control.config;

import com.example.dataplatform.shared.db.JdbcOperationalRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client(ControlPlaneProperties properties) {
        S3Client.Builder builder = S3Client.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (properties.getAws().getEndpointOverride() != null && !properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }

        return builder.build();
    }

    @Bean
    public SqsClient sqsClient(ControlPlaneProperties properties) {
        SqsClient.Builder builder = SqsClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (properties.getAws().getEndpointOverride() != null && !properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }

        return builder.build();
    }

    @Bean
    public KinesisClient kinesisClient(ControlPlaneProperties properties) {
        KinesisClient.Builder builder = KinesisClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (properties.getAws().getEndpointOverride() != null && !properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }

        return builder.build();
    }

    @Bean
    public JdbcOperationalRepository jdbcOperationalRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcOperationalRepository(jdbcTemplate);
    }
}
