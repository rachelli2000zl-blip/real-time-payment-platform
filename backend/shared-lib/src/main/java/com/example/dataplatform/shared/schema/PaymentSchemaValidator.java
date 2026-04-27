package com.example.dataplatform.shared.schema;

import com.example.dataplatform.shared.config.JacksonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class PaymentSchemaValidator {

    private final JsonSchema schema;
    private final ObjectMapper objectMapper;

    public PaymentSchemaValidator() {
        this("shared/schemas/payment/v1.json");
    }

    public PaymentSchemaValidator(String schemaPath) {
        this.objectMapper = JacksonSupport.objectMapper();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(loadSchema(schemaPath));
    }

    public void validate(String payloadJson) {
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            Set<ValidationMessage> errors = schema.validate(node);
            if (!errors.isEmpty()) {
                String message = errors.stream()
                        .map(ValidationMessage::getMessage)
                        .sorted()
                        .collect(Collectors.joining("; "));
                throw new SchemaValidationException("Schema validation failed: " + message);
            }
        } catch (IOException exception) {
            throw new SchemaValidationException("Invalid JSON payload", exception);
        }
    }

    private JsonNode loadSchema(String schemaPath) {
        try {
            Path path = Path.of(schemaPath);
            if (Files.exists(path)) {
                return objectMapper.readTree(Files.newInputStream(path));
            }

            InputStream inputStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("schemas/payment/v1.json");
            if (inputStream == null) {
                throw new SchemaValidationException("Schema file not found at " + schemaPath);
            }
            return objectMapper.readTree(inputStream);
        } catch (IOException exception) {
            throw new SchemaValidationException("Unable to load schema", exception);
        }
    }
}
