CREATE TABLE IF NOT EXISTS outbox_events
(
    id           BIGSERIAL PRIMARY KEY,
    aggregate_id BIGINT       NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    status       VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    retry_count  INT          NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE INDEX idx_outbox_status ON outbox_events (status);
CREATE INDEX idx_outbox_created_at ON outbox_events (created_at);
