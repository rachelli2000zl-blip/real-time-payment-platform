package com.example.dataplatform.retry.worker;

import com.example.dataplatform.retry.config.RetryWorkerProperties;
import com.example.dataplatform.shared.config.JacksonSupport;
import com.example.dataplatform.shared.model.ProcessingContext;
import com.example.dataplatform.shared.model.ProcessingOutcome;
import com.example.dataplatform.shared.model.RetryMessage;
import com.example.dataplatform.shared.processing.PaymentProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Instant;
import java.util.List;

@Component
public class RetryPollingWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryPollingWorker.class);

    private final RetryWorkerProperties properties;
    private final SqsClient sqsClient;
    private final PaymentProcessingService processingService;
    private final ObjectMapper objectMapper = JacksonSupport.objectMapper();

    public RetryPollingWorker(
            RetryWorkerProperties properties,
            SqsClient sqsClient,
            PaymentProcessingService processingService
    ) {
        this.properties = properties;
        this.sqsClient = sqsClient;
        this.processingService = processingService;
    }

    @Scheduled(fixedDelayString = "${app.worker.poll-interval-ms:4000}")
    public void poll() {
        String queueUrl = properties.getAws().getRetryQueueUrl();
        if (queueUrl == null || queueUrl.isBlank()) {
            return;
        }

        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(properties.getWorker().getMaxMessagesPerPoll())
                .waitTimeSeconds(10)
                .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();

        for (Message message : messages) {
            handleMessage(queueUrl, message);
        }
    }

    private void handleMessage(String queueUrl, Message message) {
        try {
            RetryMessage retryMessage = objectMapper.readValue(message.body(), RetryMessage.class);

            if (retryMessage.nextAttemptAt() != null && retryMessage.nextAttemptAt().isAfter(Instant.now())) {
                int visibilitySeconds = (int) Math.min(
                        43200,
                        retryMessage.nextAttemptAt().getEpochSecond() - Instant.now().getEpochSecond()
                );
                sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .visibilityTimeout(Math.max(1, visibilitySeconds))
                        .build());
                return;
            }

            ProcessingContext context = ProcessingContext.fromRetryWorker(retryMessage.attempts());
            ProcessingOutcome outcome = processingService.process(retryMessage.payloadJson(), context);

            if (outcome != ProcessingOutcome.FAILED) {
                deleteMessage(queueUrl, message.receiptHandle());
            }

            LOGGER.info("retry_processed eventId={} attempts={} outcome={}",
                    retryMessage.eventId(),
                    retryMessage.attempts(),
                    outcome);
        } catch (Exception exception) {
            LOGGER.error("retry_message_failure messageId={} error={}", message.messageId(), exception.getMessage(), exception);
        }
    }

    private void deleteMessage(String queueUrl, String receiptHandle) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build());
    }
}
