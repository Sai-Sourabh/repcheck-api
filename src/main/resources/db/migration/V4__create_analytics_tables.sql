-- One row per exercise per session. Pre-computed on save so the
-- up / down / same arrow is a plain read.
CREATE TABLE exercise_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_id     UUID    NOT NULL REFERENCES exercises (id),
    session_id      UUID    NOT NULL REFERENCES workout_sessions (id) ON DELETE CASCADE,
    session_date    DATE    NOT NULL,
    best_1rm        DOUBLE PRECISION NOT NULL,
    best_weight_kg  DOUBLE PRECISION NOT NULL,
    best_reps       INTEGER NOT NULL,
    total_volume_kg DOUBLE PRECISION NOT NULL,
    total_sets      INTEGER NOT NULL,
    delta_pct       DOUBLE PRECISION,
    delta_direction VARCHAR(10) NOT NULL,
    is_pr           BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_history_lookup ON exercise_history (user_id, exercise_id, session_date DESC);
CREATE INDEX idx_history_user_date ON exercise_history (user_id, session_date);

CREATE TABLE daily_load (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    load_date       DATE NOT NULL,
    total_volume_kg DOUBLE PRECISION NOT NULL,
    atl             DOUBLE PRECISION NOT NULL,
    ctl             DOUBLE PRECISION NOT NULL,
    tsb             DOUBLE PRECISION NOT NULL,
    UNIQUE (user_id, load_date)
);

CREATE TABLE muscle_weekly_volume (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    muscle_group    VARCHAR(30) NOT NULL,
    week_start      DATE        NOT NULL,
    total_sets      DOUBLE PRECISION NOT NULL,
    total_volume_kg DOUBLE PRECISION NOT NULL,
    recommendation  VARCHAR(20) NOT NULL,
    reason          VARCHAR(255),
    suggested_sets  INTEGER,
    UNIQUE (user_id, muscle_group, week_start)
);
