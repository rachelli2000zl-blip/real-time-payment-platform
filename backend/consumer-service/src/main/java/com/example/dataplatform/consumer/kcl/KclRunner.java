package com.example.dataplatform.consumer.kcl;

import com.example.dataplatform.consumer.config.ConsumerProperties;
import com.example.dataplatform.shared.processing.PaymentProcessingService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.coordinator.Scheduler;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class KclRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(KclRunner.class);

    private final ConsumerProperties properties;
    private final PaymentProcessingService processingService;
    private final KinesisAsyncClient kinesisAsyncClient;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final CloudWatchAsyncClient cloudWatchAsyncClient;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Scheduler scheduler;

    public KclRunner(
            ConsumerProperties properties,
            PaymentProcessingService processingService,
            KinesisAsyncClient kinesisAsyncClient,
            DynamoDbAsyncClient dynamoDbAsyncClient,
            CloudWatchAsyncClient cloudWatchAsyncClient
    ) {
        this.properties = properties;
        this.processingService = processingService;
        this.kinesisAsyncClient = kinesisAsyncClient;
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.cloudWatchAsyncClient = cloudWatchAsyncClient;
    }

    @PostConstruct
    public void start() {
        String workerId = properties.getKcl().getWorkerId();
        if (workerId == null || workerId.isBlank()) {
            workerId = "worker-" + UUID.randomUUID();
        }

        ConfigsBuilder configsBuilder = new ConfigsBuilder(
                properties.getKcl().getStreamName(),
                properties.getKcl().getApplicationName(),
                kinesisAsyncClient,
                dynamoDbAsyncClient,
                cloudWatchAsyncClient,
                workerId,
                new PaymentRecordProcessorFactory(processingService)
        );

        this.scheduler = new Scheduler(
                configsBuilder.checkpointConfig(),
                configsBuilder.coordinatorConfig(),
                configsBuilder.leaseManagementConfig(),
                configsBuilder.lifecycleConfig(),
                configsBuilder.metricsConfig(),
                configsBuilder.processorConfig(),
                configsBuilder.retrievalConfig()
        );

        executorService.submit(scheduler);
        LOGGER.info(
                "kcl_scheduler_started streamName={} applicationName={} workerId={}",
                properties.getKcl().getStreamName(),
                properties.getKcl().getApplicationName(),
                workerId
        );
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.startGracefulShutdown();
        }
        executorService.shutdownNow();
        LOGGER.info("kcl_scheduler_stopped");
    }
}
