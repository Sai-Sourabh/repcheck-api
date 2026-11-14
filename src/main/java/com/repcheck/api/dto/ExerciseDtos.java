package com.repcheck.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class ExerciseDtos {

    private ExerciseDtos() {
    }

    public record ExerciseResponse(UUID id, String name, String muscleGroup, List<String> secondaryMuscles,
                                   String equipment, String category, boolean custom) {
    }

    public record CreateExerciseRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank String muscleGroup,
            List<String> secondaryMuscles,
            @NotBlank String equipment,
            @NotBlank String category) {
    }
}
