-- What the customer sees in the app (the in-app channel). event_id is the outbox event that caused it: unique, so
-- processing the same event twice can never show the same notification twice.
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user (id),
    type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    data JSONB NOT NULL DEFAULT '{}',
    event_id UUID UNIQUE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- the list, newest first (keyset by id), and the unread count
CREATE INDEX idx_notification_user ON notification (user_id, id DESC);
CREATE INDEX idx_notification_unread ON notification (user_id) WHERE read_at IS NULL;

-- Only what the user chose: no row means "enabled", so a new kind of notification reaches everyone until they opt out.
CREATE TABLE notification_preference (
    user_id BIGINT NOT NULL REFERENCES app_user (id),
    type VARCHAR(40) NOT NULL,
    channel VARCHAR(10) NOT NULL CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH')),
    enabled BOOLEAN NOT NULL,
    PRIMARY KEY (user_id, type, channel)
);

-- Where to send a push. The token is unique: a device that signs in as someone else moves to that person.
CREATE TABLE device_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user (id),
    token VARCHAR(255) NOT NULL UNIQUE,
    platform VARCHAR(10) NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_device_token_user ON device_token (user_id);

-- Idempotency by event and channel: the same event, delivered twice by the queue, sends each channel once. PENDING is a
-- delivery that started and has not been confirmed (a crash, or a failure): it is tried again; SENT and SKIPPED are done.
CREATE TABLE notification_delivery (
    event_id UUID NOT NULL,
    channel VARCHAR(10) NOT NULL CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH')),
    status VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'SKIPPED')),
    attempts INTEGER NOT NULL DEFAULT 1,
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (event_id, channel)
);
