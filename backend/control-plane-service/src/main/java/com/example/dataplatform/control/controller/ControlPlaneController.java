package com.example.dataplatform.control.controller;

import com.example.dataplatform.control.config.ControlPlaneProperties;
import com.example.dataplatform.control.dto.DlqReplayRequest;
import com.example.dataplatform.control.dto.ReplayResponse;
import com.example.dataplatform.control.dto.SummaryResponse;
import com.example.dataplatform.control.service.DlqService;
import com.example.dataplatform.control.service.ErrorService;
import com.example.dataplatform.control.service.ReplayService;
import com.example.dataplatform.control.service.SchemaService;
import com.example.dataplatform.control.service.SummaryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping
public class ControlPlaneController {

    private final SummaryService summaryService;
    private final ErrorService errorService;
    private final DlqService dlqService;
    private final ReplayService replayService;
    private final SchemaService schemaService;
    private final ControlPlaneProperties properties;

    public ControlPlaneController(
            SummaryService summaryService,
            ErrorService errorService,
            DlqService dlqService,
            ReplayService replayService,
            SchemaService schemaService,
            ControlPlaneProperties properties
    ) {
        this.summaryService = summaryService;
        this.errorService = errorService;
        this.dlqService = dlqService;
        this.replayService = replayService;
        this.schemaService = schemaService;
        this.properties = properties;
    }

    @GetMapping("/summary")
    public SummaryResponse summary() {
        return summaryService.getSummary();
    }

    @GetMapping("/errors")
    public Object errors(
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String stage,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return errorService.listErrors(eventId, stage, limit);
    }

    @GetMapping("/dlq")
    public Object dlq(@RequestParam(defaultValue = "100") int limit) {
        return dlqService.listDlqEvents(limit);
    }

    @PostMapping("/dlq/replay")
    public ReplayResponse replayDlq(@RequestBody DlqReplayRequest request) {
        int count = dlqService.replay(request);
        return new ReplayResponse(count, "replayed");
    }

    @PostMapping("/replay/by-date")
    public ReplayResponse replayByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String eventType,
            @RequestParam(defaultValue = "1000") int maxRecords,
            @RequestParam(defaultValue = "dashboard") String actor
    ) {
        int count = replayService.replayByDate(date, eventType, maxRecords, actor);
        return new ReplayResponse(count, "replayed");
    }

    @GetMapping("/schemas")
    public Object schemas() {
        return schemaService.schemas();
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of(
                "maxReplayRecords", properties.getReplay().getMaxRecords(),
                "maxAttempts", properties.getProcessing().getMaxAttempts(),
                "stages", new String[]{"ingestion", "kinesis-consumer", "retry-worker"}
        );
    }
}
