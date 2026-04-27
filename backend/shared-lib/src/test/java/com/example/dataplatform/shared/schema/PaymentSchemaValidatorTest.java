package com.example.dataplatform.shared.schema;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentSchemaValidatorTest {

    private final PaymentSchemaValidator validator = new PaymentSchemaValidator("shared/schemas/payment/v1.json");

    @Test
    void validatesValidPayload() {
        String payload = """
                {
                  "eventId": "5ca37e90-70a5-445b-8644-f8ca6b5f7478",
                  "eventType": "payment.created",
                  "occurredAt": "2026-01-15T10:15:30Z",
                  "customerId": "cust-123",
                  "amount": 12.50,
                  "currency": "USD",
                  "schemaVersion": 1,
                  "metadata": {"source": "web"}
                }
                """;

        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidEventType() {
        String payload = """
                {
                  "eventId": "5ca37e90-70a5-445b-8644-f8ca6b5f7478",
                  "eventType": "payment.unknown",
                  "occurredAt": "2026-01-15T10:15:30Z",
                  "customerId": "cust-123",
                  "amount": 12.50,
                  "currency": "USD",
                  "schemaVersion": 1,
                  "metadata": {"source": "web"}
                }
                """;

        assertThatThrownBy(() -> validator.validate(payload))
                .isInstanceOf(SchemaValidationException.class)
                .hasMessageContaining("Schema validation failed");
    }
}
