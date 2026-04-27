CREATE EXTERNAL TABLE IF NOT EXISTS payments_processed_events_json (
  eventId string,
  eventType string,
  occurredAt string,
  customerId string,
  amount double,
  currency string,
  schemaVersion int,
  metadata string,
  requestId string,
  receivedAt string
)
PARTITIONED BY (
  event_type string,
  dt string
)
ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'
LOCATION 's3://<bucket>/processed/'
TBLPROPERTIES ('has_encrypted_data'='false');

-- Run after new partitions arrive:
MSCK REPAIR TABLE payments_processed_events_json;

-- Enhancement recommendation:
-- Convert processed JSON to Parquet via Glue ETL for lower Athena scan cost and faster queries.
