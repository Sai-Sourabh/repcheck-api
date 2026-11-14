CREATE TABLE exercises (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(120) NOT NULL,
    muscle_group      VARCHAR(30)  NOT NULL,
    secondary_muscles VARCHAR(255),
    equipment         VARCHAR(40)  NOT NULL,
    category          VARCHAR(20)  NOT NULL,
    is_custom         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by        UUID REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_exercises_muscle ON exercises (muscle_group);
CREATE INDEX idx_exercises_created_by ON exercises (created_by);
