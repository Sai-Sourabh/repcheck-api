package com.repcheck.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class WorkoutDtos {

    private WorkoutDtos() {
    }

    public record SetRequest(
            @NotNull UUID exerciseId,
            @PositiveOrZero @Max(1000) double weightKg,
            @Min(1) @Max(500) int reps,
            /** Reps done with good form. Leave out when every rep was clean. */
            @Min(0) @Max(500) Integer cleanReps,
            @PositiveOrZero Integer restSeconds) {
    }

    public record CreateWorkoutRequest(
            UUID templateId,
            @Size(max = 120) String name,
            LocalDate sessionDate,
            @PositiveOrZero @Max(1440) Integer durationMinutes,
            @Size(max = 2000) String notes,
            @NotEmpty @Valid List<SetRequest> sets) {
    }

    /** cleanReps is null when every rep was clean. estimatedOneRm is based on clean reps only. */
    public record SetResponse(int setNumber, double weightKg, int reps, Integer cleanReps,
                              double estimatedOneRm, Integer restSeconds) {
    }

    public record ExerciseBlock(UUID exerciseId, String exerciseName, String muscleGroup, double bestOneRm,
                                Double deltaPct, String deltaDirection, boolean pr, Double formPct,
                                List<SetResponse> sets) {
    }

    public record WorkoutDetail(UUID id, String name, LocalDate sessionDate, Integer durationMinutes,
                                String notes, double totalVolumeKg, UUID templateId,
                                List<ExerciseBlock> exercises) {
    }

    public record WorkoutSummary(UUID id, String name, LocalDate sessionDate, Integer durationMinutes,
                                 double totalVolumeKg, int exerciseCount, int setCount,
                                 int upCount, int downCount, int prCount) {
    }

    /** What the user did last time on an exercise, shown as a hint while logging. */
    public record PreviousPerformance(UUID exerciseId, LocalDate sessionDate, double bestOneRm,
                                      List<SetResponse> sets) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long total) {
    }
}
