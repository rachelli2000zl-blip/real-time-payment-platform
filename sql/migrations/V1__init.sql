CREATE TABLE IF NOT EXISTS processed_events (
    event_id TEXT PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL,
    partition_key TEXT,
    shard_id TEXT,
    sequence_number TEXT
);

CREATE TABLE IF NOT EXISTS processing_errors (
    id UUID PRIMARY KEY,
    event_id TEXT,
    stage TEXT NOT NULL,
    error_message TEXT NOT NULL,
    stack TEXT,
    attempts INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_attempt_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_processing_errors_event_id ON processing_errors (event_id);
CREATE INDEX IF NOT EXISTS idx_processing_errors_created_at ON processing_errors (created_at DESC);

CREATE TABLE IF NOT EXISTS dlq_events (
    id UUID PRIMARY KEY,
    event_id TEXT,
    payload_json JSONB NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dlq_events_created_at ON dlq_events (created_at DESC);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    details_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);
