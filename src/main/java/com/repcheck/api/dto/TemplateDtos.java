package com.repcheck.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TemplateDtos {

    private TemplateDtos() {
    }

    public record DayExerciseRequest(
            @NotNull UUID exerciseId,
            @Min(1) @Max(20) int targetSets,
            @Min(1) @Max(100) int targetReps) {
    }

    public record DayRequest(@NotBlank @Size(max = 60) String name, @Valid List<DayExerciseRequest> exercises) {
    }

    public record TemplateRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 40) String splitType,
            @NotEmpty @Valid List<DayRequest> days) {
    }

    public record DayExerciseResponse(UUID exerciseId, String exerciseName, String muscleGroup,
                                      int targetSets, int targetReps) {
    }

    public record DayResponse(String name, List<DayExerciseResponse> exercises) {
    }

    public record TemplateResponse(UUID id, String name, String splitType, List<DayResponse> days,
                                   Instant createdAt) {
    }
}
