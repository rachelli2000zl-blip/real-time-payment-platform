package com.example.dataplatform.consumer.config;

import com.example.dataplatform.shared.db.JdbcOperationalRepository;
import com.example.dataplatform.shared.db.JdbcProcessedEventRepository;
import com.example.dataplatform.shared.processing.PaymentProcessingService;
import com.example.dataplatform.shared.queue.FailureRouter;
import com.example.dataplatform.shared.s3.S3DataLakeWriter;
import com.example.dataplatform.shared.schema.PaymentSchemaValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class ProcessingConfig {

    @Bean
    public PaymentSchemaValidator paymentSchemaValidator(ConsumerProperties properties) {
        return new PaymentSchemaValidator(properties.getSchema().getPath());
    }

    @Bean
    public JdbcProcessedEventRepository jdbcProcessedEventRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcProcessedEventRepository(jdbcTemplate);
    }

    @Bean
    public JdbcOperationalRepository jdbcOperationalRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcOperationalRepository(jdbcTemplate);
    }

    @Bean
    public S3DataLakeWriter s3DataLakeWriter(S3Client s3Client, ConsumerProperties properties) {
        return new S3DataLakeWriter(s3Client, properties.getAws().getDataLakeBucket());
    }

    @Bean
    public FailureRouter failureRouter(
            SqsClient sqsClient,
            JdbcOperationalRepository operationalRepository,
            ConsumerProperties properties
    ) {
        return new FailureRouter(
                sqsClient,
                operationalRepository,
                properties.getAws().getRetryQueueUrl(),
                properties.getAws().getDlqQueueUrl(),
                properties.getProcessing().getMaxAttempts()
        );
    }

    @Bean
    public PaymentProcessingService paymentProcessingService(
            PaymentSchemaValidator schemaValidator,
            JdbcProcessedEventRepository processedEventRepository,
            JdbcOperationalRepository operationalRepository,
            S3DataLakeWriter dataLakeWriter,
            FailureRouter failureRouter
    ) {
        return new PaymentProcessingService(
                schemaValidator,
                processedEventRepository,
                operationalRepository,
                dataLakeWriter,
                failureRouter
        );
    }
}
