package com.example.dataplatform.consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsClientConfig {

    @Bean
    public KinesisAsyncClient kinesisAsyncClient(ConsumerProperties properties) {
        KinesisAsyncClient.Builder builder = KinesisAsyncClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient(ConsumerProperties properties) {
        DynamoDbAsyncClient.Builder builder = DynamoDbAsyncClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient(ConsumerProperties properties) {
        CloudWatchAsyncClient.Builder builder = CloudWatchAsyncClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public S3Client s3Client(ConsumerProperties properties) {
        S3Client.Builder builder = S3Client.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public SqsClient sqsClient(ConsumerProperties properties) {
        SqsClient.Builder builder = SqsClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }
        return builder.build();
    }
}
