package com.repcheck.api.repository;

/** A logged set together with the muscles its exercise trains. */
public record SetWithExercise(String muscleGroup, String secondaryMuscles, String category,
                              Double weightKg, Integer reps) {
}
