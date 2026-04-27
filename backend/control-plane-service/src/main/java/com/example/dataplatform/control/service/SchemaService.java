package com.example.dataplatform.control.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SchemaService {

    public List<Map<String, Object>> schemas() {
        return List.of(
                Map.of(
                        "name", "payment",
                        "version", 1,
                        "path", "shared/schemas/payment/v1.json"
                )
        );
    }
}
