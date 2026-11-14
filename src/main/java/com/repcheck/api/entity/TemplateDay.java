package com.repcheck.api.entity;

import java.util.List;
import java.util.UUID;

/** One day of a split, stored inside workout_templates.days (JSONB). */
public record TemplateDay(String name, List<TemplateExercise> exercises) {

    public record TemplateExercise(UUID exerciseId, int targetSets, int targetReps) {
    }
}
