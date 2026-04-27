package com.example.dataplatform.shared.model;

public enum ProcessingOutcome {
    SUCCESS,
    DUPLICATE,
    RETRY_ENQUEUED,
    SENT_TO_DLQ,
    FAILED
}
