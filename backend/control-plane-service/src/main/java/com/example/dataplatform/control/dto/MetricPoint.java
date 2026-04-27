package com.example.dataplatform.control.dto;

public record MetricPoint(
        String minute,
        long processed,
        long errors
) {
}
