package com.repcheck.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    /** Latest state of one lift: the "stock ticker" card. */
    public record LiftCard(UUID exerciseId, String exerciseName, String muscleGroup, LocalDate sessionDate,
                           double bestOneRm, double bestWeightKg, int bestReps, Double deltaPct,
                           String deltaDirection, boolean pr, Double formPct, String formWarning,
                           Double suggestedWeightKg, List<Double> trend) {
    }

    public record HistoryPoint(LocalDate sessionDate, double bestOneRm, double bestWeightKg, int bestReps,
                               double totalVolumeKg, Double deltaPct, String deltaDirection, boolean pr,
                               Double formPct) {
    }

    public record MuscleVolume(String muscleGroup, double totalSets, double totalVolumeKg,
                               double previousWeekSets, Integer targetSets, int mev, int mav, int mrv,
                               String recommendation, String reason, Integer suggestedSets) {
    }

    /** complete = the week has ended, so the recommendations are final. */
    public record WeeklyVolumeResponse(LocalDate weekStart, LocalDate weekEnd, boolean complete,
                                       double totalSets, double totalVolumeKg, List<MuscleVolume> muscles) {
    }

    public record FatiguePoint(LocalDate date, double volumeKg, double atl, double ctl, double tsb) {
    }

    public record FatigueResponse(String status, String message, Double tsbPercent,
                                  List<FatiguePoint> points) {
    }

    public record HeatmapDay(LocalDate date, double volumeKg, int sessions) {
    }

    public record MuscleSets(String muscleGroup, double sets) {
    }

    public record BalanceResponse(double pushSets, double pullSets, double legSets, double coreSets,
                                  Double pushPullRatio, List<MuscleSets> muscles) {
    }

    public record PrRecord(UUID exerciseId, String exerciseName, String muscleGroup, LocalDate sessionDate,
                           double bestOneRm, double bestWeightKg, int bestReps) {
    }
}
