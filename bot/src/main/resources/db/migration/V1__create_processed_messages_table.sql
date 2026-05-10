CREATE TABLE IF NOT EXISTS processed_messages
(
    message_id BIGINT PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_messages_processed_at ON processed_messages (processed_at);
