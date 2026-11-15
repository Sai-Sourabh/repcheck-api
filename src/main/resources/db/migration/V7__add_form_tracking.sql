-- Form check: how many reps of a set were done with good form.
-- NULL means every rep was clean.
ALTER TABLE session_sets ADD COLUMN clean_reps INTEGER;

ALTER TABLE exercise_history
    ADD COLUMN total_reps    INTEGER          NOT NULL DEFAULT 0,
    ADD COLUMN clean_reps    INTEGER          NOT NULL DEFAULT 0,
    ADD COLUMN top_weight_kg DOUBLE PRECISION NOT NULL DEFAULT 0;
