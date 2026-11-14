CREATE TABLE workout_templates (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(120) NOT NULL,
    split_type VARCHAR(40),
    days       JSONB        NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_templates_user ON workout_templates (user_id);

CREATE TABLE workout_sessions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    template_id      UUID        REFERENCES workout_templates (id) ON DELETE SET NULL,
    name             VARCHAR(120),
    session_date     DATE        NOT NULL,
    duration_minutes INTEGER,
    notes            TEXT,
    total_volume_kg  DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_sessions_user_date ON workout_sessions (user_id, session_date DESC);

CREATE TABLE session_sets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id    UUID    NOT NULL REFERENCES workout_sessions (id) ON DELETE CASCADE,
    exercise_id   UUID    NOT NULL REFERENCES exercises (id),
    seq           INTEGER NOT NULL,
    set_number    INTEGER NOT NULL,
    weight_kg     DOUBLE PRECISION NOT NULL,
    reps          INTEGER NOT NULL,
    estimated_1rm DOUBLE PRECISION NOT NULL,
    rest_seconds  INTEGER
);
CREATE INDEX idx_sets_session ON session_sets (session_id);
CREATE INDEX idx_sets_exercise ON session_sets (exercise_id);
