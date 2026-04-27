package com.example.dataplatform.retry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Bean
    public SqsClient sqsClient(RetryWorkerProperties properties) {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }

        return builder.build();
    }

    @Bean
    public S3Client s3Client(RetryWorkerProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }

        return builder.build();
    }
}
