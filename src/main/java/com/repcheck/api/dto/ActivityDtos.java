package com.repcheck.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Streaks, cardio / steps and body weight. */
public final class ActivityDtos {

    private ActivityDtos() {
    }

    public record StreakResponse(String type, int currentStreak, int longestStreak, int freezeTokens,
                                 int weekProgress, int weekGoal, boolean goalMetThisWeek) {
    }

    public record StreaksResponse(StreakResponse workout, StreakResponse steps) {
    }

    public record CardioRequest(
            LocalDate sessionDate,
            @NotBlank String activityType,
            @PositiveOrZero @Max(1000) Double distanceKm,
            @PositiveOrZero @Max(1440) Integer durationMinutes,
            @PositiveOrZero @Max(200000) Integer steps,
            @Min(30) @Max(250) Integer avgHeartRate) {
    }

    public record CardioResponse(UUID id, LocalDate sessionDate, String activityType, Double distanceKm,
                                 Integer durationMinutes, Integer steps, Integer avgHeartRate,
                                 Double paceMinPerKm) {
    }

    public record DaySteps(LocalDate date, int steps) {
    }

    public record StepsWeekResponse(LocalDate weekStart, int goal, int totalSteps, List<DaySteps> days) {
    }

    public record BodyWeightRequest(LocalDate logDate, @Positive @Max(500) double weightKg) {
    }

    public record BodyWeightResponse(LocalDate logDate, double weightKg) {
    }
}
