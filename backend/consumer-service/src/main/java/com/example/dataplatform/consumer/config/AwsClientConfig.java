package com.example.dataplatform.consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
public class AwsClientConfig {

    @Bean
    public KinesisAsyncClient kinesisAsyncClient(ConsumerProperties properties) {
        KinesisAsyncClientBuilder builder = KinesisAsyncClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient(ConsumerProperties properties) {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient(ConsumerProperties properties) {
        CloudWatchAsyncClientBuilder builder = CloudWatchAsyncClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public S3Client s3Client(ConsumerProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public SqsClient sqsClient(ConsumerProperties properties) {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (!properties.getAws().getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()));
        }
        return builder.build();
    }
}
