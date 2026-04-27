package com.example.dataplatform.shared.model;

public record ProcessingContext(
        String stage,
        int attempts,
        String partitionKey,
        String shardId,
        String sequenceNumber,
        boolean routeOnFailure
) {

    public static ProcessingContext fromKinesis(String shardId, String partitionKey, String sequenceNumber, int attempts) {
        return new ProcessingContext("kinesis-consumer", attempts, partitionKey, shardId, sequenceNumber, true);
    }

    public static ProcessingContext fromRetryWorker(int attempts) {
        return new ProcessingContext("retry-worker", attempts, null, null, null, true);
    }
}
