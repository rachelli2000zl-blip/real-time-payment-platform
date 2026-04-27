package com.example.dataplatform.consumer.kcl;

import com.example.dataplatform.shared.processing.PaymentProcessingService;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

public class PaymentRecordProcessorFactory implements ShardRecordProcessorFactory {

    private final PaymentProcessingService processingService;

    public PaymentRecordProcessorFactory(PaymentProcessingService processingService) {
        this.processingService = processingService;
    }

    @Override
    public ShardRecordProcessor shardRecordProcessor() {
        return new PaymentRecordProcessor(processingService);
    }
}
