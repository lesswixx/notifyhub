CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(255) UNIQUE NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL DEFAULT 'USER',
    telegram_chat_id VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_type VARCHAR(50)  NOT NULL,
    params      TEXT,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS rules (
    id                    BIGSERIAL PRIMARY KEY,
    subscription_id       BIGINT       NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    keyword_filter        VARCHAR(500),
    dedup_window_minutes  INT          NOT NULL DEFAULT 0,
    rate_limit_per_hour   INT          NOT NULL DEFAULT 0,
    priority              VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    quiet_hours_start     TIME,
    quiet_hours_end       TIME,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS events (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type  VARCHAR(50)  NOT NULL,
    external_id  VARCHAR(500),
    title        VARCHAR(1000),
    payload_json TEXT,
    priority     VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id    UUID         REFERENCES events(id),
    channel     VARCHAR(50)  NOT NULL,
    status      VARCHAR(50)  NOT NULL DEFAULT 'CREATED',
    attempts    INT          NOT NULL DEFAULT 0,
    last_error  TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id   ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_enabled   ON subscriptions(enabled);
CREATE INDEX IF NOT EXISTS idx_rules_subscription_id   ON rules(subscription_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id   ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status    ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_events_source_type      ON events(source_type);
CREATE INDEX IF NOT EXISTS idx_events_external_id      ON events(external_id);
CREATE INDEX IF NOT EXISTS idx_events_created_at       ON events(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_created   ON notifications(created_at);
