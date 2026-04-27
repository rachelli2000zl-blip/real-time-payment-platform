package com.example.dataplatform.ingestion.controller;

import com.example.dataplatform.ingestion.dto.EventAcceptedResponse;
import com.example.dataplatform.ingestion.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping
public class EventController {

    private final IngestionService ingestionService;

    public EventController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/events")
    public ResponseEntity<EventAcceptedResponse> ingest(@RequestBody String payloadJson) {
        String requestId = UUID.randomUUID().toString();
        ingestionService.ingest(payloadJson, requestId);
        return ResponseEntity.accepted().body(new EventAcceptedResponse(requestId, "accepted"));
    }
}
