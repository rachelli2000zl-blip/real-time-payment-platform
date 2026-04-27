package com.example.dataplatform.control.dto;

import java.time.Instant;
import java.util.List;

public record SummaryResponse(
        double throughputPerMinute,
        double errorRate,
        long dlqDepth,
        long approximateLagSeconds,
        Instant latestProcessedTime,
        List<MetricPoint> series
) {
}
