package com.example.dataplatform.consumer.kcl;

import com.example.dataplatform.shared.model.ProcessingContext;
import com.example.dataplatform.shared.model.ProcessingOutcome;
import com.example.dataplatform.shared.processing.PaymentProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PaymentRecordProcessor implements ShardRecordProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentRecordProcessor.class);

    private final PaymentProcessingService processingService;
    private String shardId;

    public PaymentRecordProcessor(PaymentProcessingService processingService) {
        this.processingService = processingService;
    }

    @Override
    public void initialize(InitializationInput initializationInput) {
        this.shardId = initializationInput.shardId();
        LOGGER.info("shard_initialized shardId={}", shardId);
    }

    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
        for (KinesisClientRecord record : processRecordsInput.records()) {
            try {
                ByteBuffer data = record.data().asReadOnlyBuffer();
                String payload = StandardCharsets.UTF_8.decode(data).toString();
                ProcessingContext context = ProcessingContext.fromKinesis(
                        shardId,
                        record.partitionKey(),
                        record.sequenceNumber(),
                        0
                );
                ProcessingOutcome outcome = processingService.process(payload, context);
                LOGGER.info(
                        "record_processed shardId={} sequenceNumber={} partitionKey={} outcome={}",
                        shardId,
                        record.sequenceNumber(),
                        record.partitionKey(),
                        outcome
                );
            } catch (Exception exception) {
                LOGGER.error("record_processing_failure shardId={} error={}", shardId, exception.getMessage(), exception);
            }
        }

        try {
            processRecordsInput.checkpointer().checkpoint();
        } catch (ShutdownException | InvalidStateException exception) {
            LOGGER.warn("checkpoint_failed shardId={} error={}", shardId, exception.getMessage());
        }
    }

    @Override
    public void leaseLost(LeaseLostInput leaseLostInput) {
        LOGGER.info("lease_lost shardId={}", shardId);
    }

    @Override
    public void shardEnded(ShardEndedInput shardEndedInput) {
        try {
            shardEndedInput.checkpointer().checkpoint();
            LOGGER.info("shard_ended shardId={}", shardId);
        } catch (ShutdownException | InvalidStateException exception) {
            LOGGER.warn("shard_end_checkpoint_failed shardId={} error={}", shardId, exception.getMessage());
        }
    }

    @Override
    public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
        try {
            shutdownRequestedInput.checkpointer().checkpoint();
            LOGGER.info("shutdown_requested shardId={}", shardId);
        } catch (ShutdownException | InvalidStateException exception) {
            LOGGER.warn("shutdown_checkpoint_failed shardId={} error={}", shardId, exception.getMessage());
        }
    }
}
