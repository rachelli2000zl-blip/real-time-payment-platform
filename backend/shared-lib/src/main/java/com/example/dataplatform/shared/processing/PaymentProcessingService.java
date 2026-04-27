package com.example.dataplatform.shared.processing;

import com.example.dataplatform.shared.config.JacksonSupport;
import com.example.dataplatform.shared.db.JdbcOperationalRepository;
import com.example.dataplatform.shared.db.JdbcProcessedEventRepository;
import com.example.dataplatform.shared.model.PaymentEvent;
import com.example.dataplatform.shared.model.ProcessingContext;
import com.example.dataplatform.shared.model.ProcessingOutcome;
import com.example.dataplatform.shared.queue.FailureRouter;
import com.example.dataplatform.shared.s3.S3DataLakeWriter;
import com.example.dataplatform.shared.schema.PaymentSchemaValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

public class PaymentProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentProcessingService.class);

    private final PaymentSchemaValidator schemaValidator;
    private final JdbcProcessedEventRepository processedEventRepository;
    private final JdbcOperationalRepository operationalRepository;
    private final S3DataLakeWriter dataLakeWriter;
    private final FailureRouter failureRouter;
    private final ObjectMapper objectMapper;

    public PaymentProcessingService(
            PaymentSchemaValidator schemaValidator,
            JdbcProcessedEventRepository processedEventRepository,
            JdbcOperationalRepository operationalRepository,
            S3DataLakeWriter dataLakeWriter,
            FailureRouter failureRouter
    ) {
        this.schemaValidator = schemaValidator;
        this.processedEventRepository = processedEventRepository;
        this.operationalRepository = operationalRepository;
        this.dataLakeWriter = dataLakeWriter;
        this.failureRouter = failureRouter;
        this.objectMapper = JacksonSupport.objectMapper();
    }

    public ProcessingOutcome process(String payloadJson, ProcessingContext context) {
        String eventId = extractEventId(payloadJson);

        try {
            schemaValidator.validate(payloadJson);
            PaymentEvent event = objectMapper.readValue(payloadJson, PaymentEvent.class);

            boolean inserted = processedEventRepository.insertIfAbsent(
                    event.eventId(),
                    Instant.now(),
                    context.partitionKey(),
                    context.shardId(),
                    context.sequenceNumber()
            );

            if (!inserted) {
                LOGGER.info("event_duplicate eventId={} requestId={} stage={}",
                        event.eventId(),
                        event.requestId(),
                        context.stage());
                return ProcessingOutcome.DUPLICATE;
            }

            dataLakeWriter.writeProcessed(event, payloadJson);
            LOGGER.info("event_processed eventId={} requestId={} stage={} partitionKey={} sequenceNumber={}",
                    event.eventId(),
                    event.requestId(),
                    context.stage(),
                    context.partitionKey(),
                    context.sequenceNumber());
            return ProcessingOutcome.SUCCESS;
        } catch (Exception exception) {
            operationalRepository.insertProcessingError(
                    eventId,
                    context.stage(),
                    exception.getMessage(),
                    stackTrace(exception),
                    context.attempts()
            );

            if (context.routeOnFailure() && failureRouter != null) {
                ProcessingOutcome outcome = failureRouter.routeFailure(eventId, payloadJson, context.attempts(), exception.getMessage());
                LOGGER.warn("event_failed eventId={} stage={} attempts={} outcome={} error={}",
                        eventId,
                        context.stage(),
                        context.attempts(),
                        outcome,
                        exception.getMessage());
                return outcome;
            }

            LOGGER.error("event_failed eventId={} stage={} attempts={} error={}",
                    eventId,
                    context.stage(),
                    context.attempts(),
                    exception.getMessage());
            return ProcessingOutcome.FAILED;
        }
    }

    private String extractEventId(String payloadJson) {
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            JsonNode eventId = node.get("eventId");
            if (eventId == null || eventId.isNull()) {
                return "unknown";
            }
            return eventId.asText();
        } catch (Exception exception) {
            return "unknown";
        }
    }

    private String stackTrace(Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
