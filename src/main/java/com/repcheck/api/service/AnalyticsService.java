package com.repcheck.api.service;

import com.repcheck.api.domain.FatigueCalculator;
import com.repcheck.api.domain.FormCheck;
import com.repcheck.api.domain.FatigueCalculator.Point;
import com.repcheck.api.domain.FatigueCalculator.Status;
import com.repcheck.api.domain.OneRMCalculator;
import com.repcheck.api.domain.ProgressionRuleEngine;
import com.repcheck.api.domain.ProgressionRuleEngine.Recommendation;
import com.repcheck.api.domain.VolumeLandmarks;
import com.repcheck.api.domain.VolumeLandmarks.Landmarks;
import com.repcheck.api.domain.Weeks;
import com.repcheck.api.dto.AnalyticsDtos.BalanceResponse;
import com.repcheck.api.dto.AnalyticsDtos.FatiguePoint;
import com.repcheck.api.dto.AnalyticsDtos.FatigueResponse;
import com.repcheck.api.dto.AnalyticsDtos.HeatmapDay;
import com.repcheck.api.dto.AnalyticsDtos.HistoryPoint;
import com.repcheck.api.dto.AnalyticsDtos.LiftCard;
import com.repcheck.api.dto.AnalyticsDtos.MuscleSets;
import com.repcheck.api.dto.AnalyticsDtos.MuscleVolume;
import com.repcheck.api.dto.AnalyticsDtos.PrRecord;
import com.repcheck.api.dto.AnalyticsDtos.WeeklyVolumeResponse;
import com.repcheck.api.entity.DailyLoad;
import com.repcheck.api.entity.Exercise;
import com.repcheck.api.entity.ExerciseHistory;
import com.repcheck.api.entity.MuscleWeeklyVolume;
import com.repcheck.api.repository.DailyLoadRepository;
import com.repcheck.api.repository.DailyVolume;
import com.repcheck.api.repository.ExerciseHistoryRepository;
import com.repcheck.api.repository.ExerciseRepository;
import com.repcheck.api.repository.MuscleWeeklyVolumeRepository;
import com.repcheck.api.repository.SessionSetRepository;
import com.repcheck.api.repository.SetWithExercise;
import com.repcheck.api.repository.WorkoutSessionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private static final int TREND_POINTS = 8;
    private static final int BALANCE_DAYS = 28;

    private final WorkoutSessionRepository sessions;
    private final SessionSetRepository sets;
    private final ExerciseHistoryRepository histories;
    private final ExerciseRepository exercises;
    private final DailyLoadRepository dailyLoads;
    private final MuscleWeeklyVolumeRepository weeklyVolumes;
    private final Clock clock;

    public AnalyticsService(WorkoutSessionRepository sessions, SessionSetRepository sets,
                            ExerciseHistoryRepository histories, ExerciseRepository exercises,
                            DailyLoadRepository dailyLoads, MuscleWeeklyVolumeRepository weeklyVolumes,
                            Clock clock) {
        this.sessions = sessions;
        this.sets = sets;
        this.histories = histories;
        this.exercises = exercises;
        this.dailyLoads = dailyLoads;
        this.weeklyVolumes = weeklyVolumes;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- writes

    /** Called whenever a session is saved or deleted. */
    @Transactional
    public void onWorkoutChanged(UUID userId, LocalDate sessionDate) {
        recomputeDailyLoad(userId, sessionDate);
        persistWeek(userId, Weeks.startOf(sessionDate));
    }

    /**
     * Recomputes ATL / CTL / TSB from {@code fromDate} up to today. While the user is
     * still inside the seed window, or there is nothing stored to continue from, the
     * whole series is rebuilt from the first session.
     */
    @Transactional
    public void recomputeDailyLoad(UUID userId, LocalDate fromDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDate first = sessions.firstSessionDate(userId);
        if (first == null) {
            dailyLoads.deleteAllForUser(userId);
            return;
        }
        LocalDate start = fromDate.isAfter(today) ? today : fromDate;
        Optional<DailyLoad> anchor = Optional.empty();
        boolean full = !start.isAfter(first.plusDays(FatigueCalculator.SEED_DAYS));
        if (!full) {
            anchor = dailyLoads.findFirstByUserIdAndLoadDateLessThanEqualOrderByLoadDateDesc(
                    userId, start.minusDays(1));
            full = anchor.isEmpty() || anchor.get().getLoadDate().isBefore(first);
        }
        if (full) {
            start = first;
        } else {
            // Continue right after the last stored day so no gap is left behind.
            start = anchor.get().getLoadDate().plusDays(1);
        }

        Map<LocalDate, Double> volumeByDate = new HashMap<>();
        for (DailyVolume volume : sessions.dailyVolumes(userId, start)) {
            volumeByDate.put(volume.date(), volume.volumeKg());
        }
        List<Double> loads = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            loads.add(volumeByDate.getOrDefault(day, 0.0));
        }
        double startAtl;
        double startCtl;
        if (full) {
            startAtl = FatigueCalculator.seed(loads);
            startCtl = startAtl;
            dailyLoads.deleteAllForUser(userId);
        } else {
            startAtl = anchor.get().getAtl();
            startCtl = anchor.get().getCtl();
            dailyLoads.deleteFrom(userId, start);
        }

        List<Point> points = FatigueCalculator.series(startAtl, startCtl, loads);
        List<DailyLoad> rows = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            DailyLoad row = new DailyLoad();
            row.setUserId(userId);
            row.setLoadDate(start.plusDays(i));
            row.setTotalVolumeKg(loads.get(i));
            row.setAtl(OneRMCalculator.round2(points.get(i).atl()));
            row.setCtl(OneRMCalculator.round2(points.get(i).ctl()));
            row.setTsb(OneRMCalculator.round2(points.get(i).tsb()));
            rows.add(row);
        }
        dailyLoads.saveAll(rows);
        dailyLoads.flush();
    }

    /** Stores the weekly sets per muscle together with the recommendation. */
    @Transactional
    public void persistWeek(UUID userId, LocalDate weekStart) {
        weeklyVolumes.deleteWeek(userId, weekStart);
        weeklyVolumes.saveAll(buildWeek(userId, weekStart));
    }

    // ----------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public WeeklyVolumeResponse weeklyVolume(UUID userId, LocalDate anyDayInWeek) {
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = Weeks.startOf(anyDayInWeek == null ? today : anyDayInWeek);
        Map<String, MuscleWeeklyVolume> previous = new HashMap<>();
        buildWeek(userId, weekStart.minusWeeks(1)).forEach(m -> previous.put(m.getMuscleGroup(), m));

        List<MuscleVolume> muscles = new ArrayList<>();
        double totalSets = 0;
        double totalVolume = 0;
        for (MuscleWeeklyVolume row : buildWeek(userId, weekStart)) {
            Landmarks landmarks = VolumeLandmarks.forMuscle(row.getMuscleGroup());
            MuscleWeeklyVolume last = previous.get(row.getMuscleGroup());
            muscles.add(new MuscleVolume(row.getMuscleGroup(), row.getTotalSets(), row.getTotalVolumeKg(),
                    last == null ? 0 : last.getTotalSets(),
                    last == null ? null : last.getSuggestedSets(),
                    landmarks.mev(), landmarks.mav(), landmarks.mrv(),
                    row.getRecommendation(), row.getReason(), row.getSuggestedSets()));
            totalSets += row.getTotalSets();
            totalVolume += row.getTotalVolumeKg();
        }
        boolean complete = Weeks.endOf(weekStart).isBefore(today);
        return new WeeklyVolumeResponse(weekStart, Weeks.endOf(weekStart), complete, totalSets,
                OneRMCalculator.round2(totalVolume), muscles);
    }

    /** Not read-only: tops the series up to today if the nightly job has not run yet. */
    @Transactional
    public FatigueResponse fatigue(UUID userId, int days) {
        LocalDate today = LocalDate.now(clock);
        LocalDate first = sessions.firstSessionDate(userId);
        if (first == null) {
            return new FatigueResponse(Status.BUILDING_BASELINE.name(),
                    "Log a few workouts and your fatigue trend will show up here.", null, List.of());
        }
        if (dailyLoads.findByUserIdAndLoadDate(userId, today).isEmpty()) {
            recomputeDailyLoad(userId, today);
        }
        int window = Math.min(Math.max(days, 7), 365);
        List<DailyLoad> rows = dailyLoads.findByUserIdAndLoadDateGreaterThanEqualOrderByLoadDateAsc(
                userId, today.minusDays(window - 1L));
        List<FatiguePoint> points = rows.stream()
                .map(r -> new FatiguePoint(r.getLoadDate(), r.getTotalVolumeKg(), r.getAtl(), r.getCtl(), r.getTsb()))
                .toList();
        if (rows.isEmpty()) {
            return new FatigueResponse(Status.BUILDING_BASELINE.name(), message(Status.BUILDING_BASELINE),
                    null, points);
        }
        DailyLoad latest = rows.get(rows.size() - 1);
        long daysOfData = ChronoUnit.DAYS.between(first, today) + 1;
        Status status = FatigueCalculator.status(latest.getAtl(), latest.getCtl(), daysOfData);
        Double pct = FatigueCalculator.tsbPercent(latest.getAtl(), latest.getCtl());
        return new FatigueResponse(status.name(), message(status),
                pct == null ? null : OneRMCalculator.round2(pct), points);
    }

    @Transactional(readOnly = true)
    public List<HeatmapDay> heatmap(UUID userId, int days) {
        int window = Math.min(Math.max(days, 7), 730);
        LocalDate from = LocalDate.now(clock).minusDays(window - 1L);
        return sessions.dailyVolumes(userId, from).stream()
                .map(v -> new HeatmapDay(v.date(), OneRMCalculator.round2(v.volumeKg()), v.sessions().intValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public BalanceResponse balance(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        Map<String, Double> byMuscle = new LinkedHashMap<>();
        VolumeLandmarks.muscleGroups().forEach(m -> byMuscle.put(m, 0.0));
        Map<String, Double> byCategory = new HashMap<>();
        for (SetWithExercise set : sets.findWithExercise(userId, today.minusDays(BALANCE_DAYS - 1L), today)) {
            byMuscle.merge(set.muscleGroup(), 1.0, Double::sum);
            for (String secondary : splitMuscles(set.secondaryMuscles())) {
                byMuscle.merge(secondary, 0.5, Double::sum);
            }
            byCategory.merge(set.category(), 1.0, Double::sum);
        }
        double push = byCategory.getOrDefault("PUSH", 0.0);
        double pull = byCategory.getOrDefault("PULL", 0.0);
        Double ratio = pull == 0 ? null : OneRMCalculator.round2(push / pull);
        List<MuscleSets> muscles = byMuscle.entrySet().stream()
                .map(e -> new MuscleSets(e.getKey(), e.getValue()))
                .toList();
        return new BalanceResponse(push, pull, byCategory.getOrDefault("LEGS", 0.0),
                byCategory.getOrDefault("CORE", 0.0), ratio, muscles);
    }

    /** One card per exercise the user has ever logged, most recently trained first. */
    @Transactional(readOnly = true)
    public List<LiftCard> lifts(UUID userId) {
        Map<UUID, List<ExerciseHistory>> byExercise = groupByExercise(userId);
        Map<UUID, Exercise> exerciseById = loadExercises(byExercise.keySet());
        List<LiftCard> cards = new ArrayList<>();
        for (Map.Entry<UUID, List<ExerciseHistory>> entry : byExercise.entrySet()) {
            Exercise exercise = exerciseById.get(entry.getKey());
            if (exercise == null) {
                continue;
            }
            List<ExerciseHistory> chain = entry.getValue();
            ExerciseHistory latest = chain.get(chain.size() - 1);
            List<Double> trend = chain.subList(Math.max(0, chain.size() - TREND_POINTS), chain.size()).stream()
                    .map(ExerciseHistory::getBestOneRm)
                    .toList();
            FormCheck.Warning warning = FormCheck.egoLift(chain.stream()
                    .map(h -> new FormCheck.Session(h.getTopWeightKg(), h.getTotalReps(), h.getCleanReps()))
                    .toList());
            cards.add(new LiftCard(exercise.getId(), exercise.getName(), exercise.getMuscleGroup(),
                    latest.getSessionDate(), latest.getBestOneRm(), latest.getBestWeightKg(),
                    latest.getBestReps(), latest.getDeltaPct(), latest.getDeltaDirection(), latest.isPr(),
                    FormCheck.formPct(latest.getTotalReps(), latest.getCleanReps()),
                    warning == null ? null : warning.message(),
                    warning == null ? null : warning.suggestedWeightKg(),
                    trend));
        }
        cards.sort(Comparator.comparing(LiftCard::sessionDate).reversed()
                .thenComparing(LiftCard::exerciseName));
        return cards;
    }

    @Transactional(readOnly = true)
    public List<HistoryPoint> exerciseHistory(UUID userId, UUID exerciseId) {
        return histories.findByUserIdAndExerciseIdOrderBySessionDateAsc(userId, exerciseId).stream()
                .map(h -> new HistoryPoint(h.getSessionDate(), h.getBestOneRm(), h.getBestWeightKg(),
                        h.getBestReps(), h.getTotalVolumeKg(), h.getDeltaPct(), h.getDeltaDirection(), h.isPr(),
                        FormCheck.formPct(h.getTotalReps(), h.getCleanReps())))
                .toList();
    }

    /** Best ever session per exercise. */
    @Transactional(readOnly = true)
    public List<PrRecord> personalRecords(UUID userId) {
        Map<UUID, List<ExerciseHistory>> byExercise = groupByExercise(userId);
        Map<UUID, Exercise> exerciseById = loadExercises(byExercise.keySet());
        List<PrRecord> records = new ArrayList<>();
        for (Map.Entry<UUID, List<ExerciseHistory>> entry : byExercise.entrySet()) {
            Exercise exercise = exerciseById.get(entry.getKey());
            if (exercise == null) {
                continue;
            }
            ExerciseHistory best = entry.getValue().get(0);
            for (ExerciseHistory row : entry.getValue()) {
                if (row.getBestOneRm() > best.getBestOneRm()
                        || (row.getBestOneRm() == best.getBestOneRm() && row.getBestReps() > best.getBestReps())) {
                    best = row;
                }
            }
            records.add(new PrRecord(exercise.getId(), exercise.getName(), exercise.getMuscleGroup(),
                    best.getSessionDate(), best.getBestOneRm(), best.getBestWeightKg(), best.getBestReps()));
        }
        records.sort(Comparator.comparing(PrRecord::sessionDate).reversed());
        return records;
    }

    // --------------------------------------------------------------- helpers

    /** Sets, volume and recommendation for every muscle group in one week. Nothing is saved. */
    private List<MuscleWeeklyVolume> buildWeek(UUID userId, LocalDate weekStart) {
        LocalDate weekEnd = Weeks.endOf(weekStart);
        Map<String, double[]> totals = new LinkedHashMap<>();
        VolumeLandmarks.muscleGroups().forEach(m -> totals.put(m, new double[2]));
        for (SetWithExercise set : sets.findWithExercise(userId, weekStart, weekEnd)) {
            double[] primary = totals.computeIfAbsent(set.muscleGroup(), k -> new double[2]);
            primary[0] += 1;
            primary[1] += set.weightKg() * set.reps();
            for (String secondary : splitMuscles(set.secondaryMuscles())) {
                totals.computeIfAbsent(secondary, k -> new double[2])[0] += 0.5;
            }
        }

        List<ExerciseHistory> weekHistory = histories.findByUserIdAndSessionDateBetween(userId, weekStart, weekEnd);
        Set<UUID> exerciseIds = new LinkedHashSet<>();
        weekHistory.forEach(h -> exerciseIds.add(h.getExerciseId()));
        Map<UUID, Exercise> exerciseById = loadExercises(exerciseIds);
        Map<String, double[]> deltas = new HashMap<>();
        for (ExerciseHistory history : weekHistory) {
            Exercise exercise = exerciseById.get(history.getExerciseId());
            if (exercise == null || history.getDeltaPct() == null) {
                continue;
            }
            double[] sumAndCount = deltas.computeIfAbsent(exercise.getMuscleGroup(), k -> new double[2]);
            sumAndCount[0] += history.getDeltaPct();
            sumAndCount[1] += 1;
        }

        Double tsbPct = tsbPercentAt(userId, weekEnd);
        List<MuscleWeeklyVolume> rows = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : totals.entrySet()) {
            double[] delta = deltas.get(entry.getKey());
            Double avgDelta = delta == null ? null : delta[0] / delta[1];
            Recommendation recommendation = ProgressionRuleEngine.recommend(
                    VolumeLandmarks.forMuscle(entry.getKey()), entry.getValue()[0], avgDelta, tsbPct);

            MuscleWeeklyVolume row = new MuscleWeeklyVolume();
            row.setUserId(userId);
            row.setMuscleGroup(entry.getKey());
            row.setWeekStart(weekStart);
            row.setTotalSets(entry.getValue()[0]);
            row.setTotalVolumeKg(OneRMCalculator.round2(entry.getValue()[1]));
            row.setRecommendation(recommendation.action().name());
            row.setReason(recommendation.reason());
            row.setSuggestedSets(recommendation.suggestedSets());
            rows.add(row);
        }
        return rows;
    }

    /** TSB as percent of CTL on the given day, or null until there is a baseline. */
    private Double tsbPercentAt(UUID userId, LocalDate date) {
        LocalDate first = sessions.firstSessionDate(userId);
        LocalDate today = LocalDate.now(clock);
        LocalDate day = date.isAfter(today) ? today : date;
        if (first == null || ChronoUnit.DAYS.between(first, day) + 1 < FatigueCalculator.SEED_DAYS) {
            return null;
        }
        return dailyLoads.findFirstByUserIdAndLoadDateLessThanEqualOrderByLoadDateDesc(userId, day)
                .map(d -> FatigueCalculator.tsbPercent(d.getAtl(), d.getCtl()))
                .orElse(null);
    }

    private Map<UUID, List<ExerciseHistory>> groupByExercise(UUID userId) {
        Map<UUID, List<ExerciseHistory>> byExercise = new LinkedHashMap<>();
        for (ExerciseHistory history : histories.findByUserIdOrderBySessionDateAsc(userId)) {
            byExercise.computeIfAbsent(history.getExerciseId(), k -> new ArrayList<>()).add(history);
        }
        return byExercise;
    }

    private Map<UUID, Exercise> loadExercises(Set<UUID> ids) {
        Map<UUID, Exercise> byId = new HashMap<>();
        exercises.findAllById(ids).forEach(e -> byId.put(e.getId(), e));
        return byId;
    }

    private static List<String> splitMuscles(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : csv.split(",")) {
            if (!part.isBlank()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private static String message(Status status) {
        return switch (status) {
            case BUILDING_BASELINE -> "Still learning your normal training load. Give it two weeks.";
            case FRESH -> "You are fresher than usual. Good time to push for PRs.";
            case BALANCED -> "Load matches what you are used to. Keep going.";
            case FATIGUED -> "Carrying some fatigue. Normal in a hard block, watch your lifts.";
            case OVERREACHING -> "Load is far above your baseline. Plan a deload.";
        };
    }
}
