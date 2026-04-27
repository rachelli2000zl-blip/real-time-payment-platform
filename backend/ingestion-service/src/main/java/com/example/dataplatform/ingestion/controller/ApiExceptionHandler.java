package com.example.dataplatform.ingestion.controller;

import com.example.dataplatform.shared.schema.SchemaValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SchemaValidationException.class)
    public ResponseEntity<Map<String, Object>> handleSchema(SchemaValidationException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "validation_error",
                "message", exception.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "internal_error",
                "message", exception.getMessage()
        ));
    }
}
