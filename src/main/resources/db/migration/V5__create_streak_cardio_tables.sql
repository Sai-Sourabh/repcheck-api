CREATE TABLE streaks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    streak_type         VARCHAR(20) NOT NULL,
    current_streak      INTEGER     NOT NULL DEFAULT 0,
    longest_streak      INTEGER     NOT NULL DEFAULT 0,
    last_activity_date  DATE,
    last_evaluated_week DATE        NOT NULL,
    freeze_tokens       INTEGER     NOT NULL DEFAULT 1,
    UNIQUE (user_id, streak_type)
);

CREATE TABLE cardio_sessions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    session_date     DATE        NOT NULL,
    activity_type    VARCHAR(30) NOT NULL,
    distance_km      DOUBLE PRECISION,
    duration_minutes INTEGER,
    steps            INTEGER,
    avg_heart_rate   INTEGER
);
CREATE INDEX idx_cardio_user_date ON cardio_sessions (user_id, session_date DESC);

CREATE TABLE body_weight_logs (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    log_date  DATE NOT NULL,
    weight_kg DOUBLE PRECISION NOT NULL,
    UNIQUE (user_id, log_date)
);
