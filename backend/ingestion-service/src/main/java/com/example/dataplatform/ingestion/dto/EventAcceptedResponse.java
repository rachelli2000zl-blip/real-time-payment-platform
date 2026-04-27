package com.example.dataplatform.ingestion.dto;

public record EventAcceptedResponse(
        String requestId,
        String status
) {
}
