CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255),
    recipient_user_id VARCHAR(255),
    message TEXT,
    payload TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_user_id);
CREATE INDEX idx_notifications_event_type ON notifications (event_type);
