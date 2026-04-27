package com.example.dataplatform.shared.queue;

import com.example.dataplatform.shared.config.JacksonSupport;
import com.example.dataplatform.shared.db.JdbcOperationalRepository;
import com.example.dataplatform.shared.model.ProcessingOutcome;
import com.example.dataplatform.shared.model.RetryMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;

public class FailureRouter {

    private final SqsClient sqsClient;
    private final JdbcOperationalRepository operationalRepository;
    private final ObjectMapper objectMapper;
    private final String retryQueueUrl;
    private final String dlqQueueUrl;
    private final int maxAttempts;

    public FailureRouter(
            SqsClient sqsClient,
            JdbcOperationalRepository operationalRepository,
            String retryQueueUrl,
            String dlqQueueUrl,
            int maxAttempts
    ) {
        this.sqsClient = sqsClient;
        this.operationalRepository = operationalRepository;
        this.objectMapper = JacksonSupport.objectMapper();
        this.retryQueueUrl = retryQueueUrl;
        this.dlqQueueUrl = dlqQueueUrl;
        this.maxAttempts = maxAttempts;
    }

    public ProcessingOutcome routeFailure(String eventId, String payloadJson, int currentAttempts, String lastError) {
        int nextAttempts = currentAttempts + 1;

        if (nextAttempts > maxAttempts) {
            sendToQueue(dlqQueueUrl, retryMessage(eventId, payloadJson, nextAttempts, lastError), 0);
            operationalRepository.insertDlqEvent(eventId, payloadJson, "Max attempts exceeded: " + lastError);
            return ProcessingOutcome.SENT_TO_DLQ;
        }

        int delaySeconds = Math.min(900, (int) Math.pow(2, Math.min(nextAttempts, 9)) * 5);
        sendToQueue(retryQueueUrl, retryMessage(eventId, payloadJson, nextAttempts, lastError), delaySeconds);
        return ProcessingOutcome.RETRY_ENQUEUED;
    }

    private RetryMessage retryMessage(String eventId, String payloadJson, int attempts, String lastError) {
        return new RetryMessage(
                eventId,
                payloadJson,
                attempts,
                lastError,
                Instant.now().plusSeconds(Math.min(900, (int) Math.pow(2, Math.min(attempts, 9)) * 5L))
        );
    }

    private void sendToQueue(String queueUrl, RetryMessage message, int delaySeconds) {
        if (queueUrl == null || queueUrl.isBlank()) {
            return;
        }

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(toJson(message))
                .delaySeconds(delaySeconds)
                .build();
        sqsClient.sendMessage(request);
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize retry message", exception);
        }
    }
}
