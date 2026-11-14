package com.repcheck.api.service;

import com.repcheck.api.domain.OneRMCalculator;
import com.repcheck.api.dto.WorkoutDtos.CreateWorkoutRequest;
import com.repcheck.api.dto.WorkoutDtos.ExerciseBlock;
import com.repcheck.api.dto.WorkoutDtos.PageResponse;
import com.repcheck.api.dto.WorkoutDtos.PreviousPerformance;
import com.repcheck.api.dto.WorkoutDtos.SetRequest;
import com.repcheck.api.dto.WorkoutDtos.SetResponse;
import com.repcheck.api.dto.WorkoutDtos.WorkoutDetail;
import com.repcheck.api.dto.WorkoutDtos.WorkoutSummary;
import com.repcheck.api.entity.Exercise;
import com.repcheck.api.entity.ExerciseHistory;
import com.repcheck.api.entity.SessionSet;
import com.repcheck.api.entity.WorkoutSession;
import com.repcheck.api.exception.ApiException;
import com.repcheck.api.repository.ExerciseHistoryRepository;
import com.repcheck.api.repository.ExerciseRepository;
import com.repcheck.api.repository.SessionSetRepository;
import com.repcheck.api.repository.WorkoutSessionRepository;
import com.repcheck.api.repository.WorkoutTemplateRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutService {

    private final WorkoutSessionRepository sessions;
    private final SessionSetRepository sets;
    private final ExerciseHistoryRepository histories;
    private final ExerciseRepository exercises;
    private final WorkoutTemplateRepository templates;
    private final ExerciseService exerciseService;
    private final HistoryService historyService;
    private final AnalyticsService analyticsService;
    private final Clock clock;

    public WorkoutService(WorkoutSessionRepository sessions, SessionSetRepository sets,
                          ExerciseHistoryRepository histories, ExerciseRepository exercises,
                          WorkoutTemplateRepository templates, ExerciseService exerciseService,
                          HistoryService historyService, AnalyticsService analyticsService, Clock clock) {
        this.sessions = sessions;
        this.sets = sets;
        this.histories = histories;
        this.exercises = exercises;
        this.templates = templates;
        this.exerciseService = exerciseService;
        this.historyService = historyService;
        this.analyticsService = analyticsService;
        this.clock = clock;
    }

    /**
     * Saves a session, then in the same transaction refreshes everything derived
     * from it: per-exercise history (the arrows), daily load and weekly volume.
     */
    @Transactional
    public WorkoutDetail create(UUID userId, CreateWorkoutRequest request) {
        LocalDate today = LocalDate.now(clock);
        LocalDate date = request.sessionDate() != null ? request.sessionDate() : today;
        if (date.isAfter(today)) {
            throw ApiException.badRequest("sessionDate cannot be in the future");
        }
        Set<UUID> exerciseIds = new LinkedHashSet<>();
        request.sets().forEach(s -> exerciseIds.add(s.exerciseId()));
        exerciseService.requireVisible(userId, exerciseIds);
        if (request.templateId() != null) {
            templates.findByIdAndUserId(request.templateId(), userId)
                    .orElseThrow(() -> ApiException.notFound("Template"));
        }

        double volume = 0;
        for (SetRequest set : request.sets()) {
            volume += set.weightKg() * set.reps();
        }
        WorkoutSession session = new WorkoutSession();
        session.setUserId(userId);
        session.setTemplateId(request.templateId());
        session.setName(request.name() == null || request.name().isBlank() ? null : request.name().trim());
        session.setSessionDate(date);
        session.setDurationMinutes(request.durationMinutes());
        session.setNotes(request.notes());
        session.setTotalVolumeKg(OneRMCalculator.round2(volume));
        sessions.saveAndFlush(session);

        Map<UUID, Integer> setNumbers = new HashMap<>();
        List<SessionSet> rows = new ArrayList<>();
        for (SetRequest set : request.sets()) {
            SessionSet row = new SessionSet();
            row.setSessionId(session.getId());
            row.setExerciseId(set.exerciseId());
            row.setSequence(rows.size());
            row.setSetNumber(setNumbers.merge(set.exerciseId(), 1, Integer::sum));
            row.setWeightKg(set.weightKg());
            row.setReps(set.reps());
            row.setEstimatedOneRm(OneRMCalculator.estimate(set.weightKg(), set.reps()));
            row.setRestSeconds(set.restSeconds());
            rows.add(row);
        }
        sets.saveAll(rows);
        sets.flush();

        exerciseIds.forEach(id -> historyService.rebuild(userId, id));
        analyticsService.onWorkoutChanged(userId, date);
        return detail(userId, session.getId());
    }

    @Transactional
    public void delete(UUID userId, UUID sessionId) {
        WorkoutSession session = require(userId, sessionId);
        Set<UUID> exerciseIds = new LinkedHashSet<>();
        sets.findBySessionIdOrderBySequenceAsc(sessionId).forEach(s -> exerciseIds.add(s.getExerciseId()));
        LocalDate date = session.getSessionDate();

        sets.deleteBySessionId(sessionId);
        sessions.delete(session);
        sessions.flush();

        exerciseIds.forEach(id -> historyService.rebuild(userId, id));
        analyticsService.onWorkoutChanged(userId, date);
    }

    @Transactional(readOnly = true)
    public WorkoutDetail detail(UUID userId, UUID sessionId) {
        WorkoutSession session = require(userId, sessionId);
        List<SessionSet> sessionSets = sets.findBySessionIdOrderBySequenceAsc(sessionId);

        Map<UUID, List<SessionSet>> byExercise = new LinkedHashMap<>();
        sessionSets.forEach(s -> byExercise.computeIfAbsent(s.getExerciseId(), k -> new ArrayList<>()).add(s));
        Map<UUID, Exercise> exerciseById = loadExercises(byExercise.keySet());
        Map<UUID, ExerciseHistory> historyByExercise = new HashMap<>();
        histories.findBySessionIdIn(List.of(sessionId)).forEach(h -> historyByExercise.put(h.getExerciseId(), h));

        List<ExerciseBlock> blocks = new ArrayList<>();
        for (Map.Entry<UUID, List<SessionSet>> entry : byExercise.entrySet()) {
            Exercise exercise = exerciseById.get(entry.getKey());
            ExerciseHistory history = historyByExercise.get(entry.getKey());
            blocks.add(new ExerciseBlock(entry.getKey(),
                    exercise == null ? "Unknown" : exercise.getName(),
                    exercise == null ? null : exercise.getMuscleGroup(),
                    history == null ? 0 : history.getBestOneRm(),
                    history == null ? null : history.getDeltaPct(),
                    history == null ? "NEW" : history.getDeltaDirection(),
                    history != null && history.isPr(),
                    entry.getValue().stream().map(WorkoutService::toSetResponse).toList()));
        }
        return new WorkoutDetail(session.getId(), session.getName(), session.getSessionDate(),
                session.getDurationMinutes(), session.getNotes(), session.getTotalVolumeKg(),
                session.getTemplateId(), blocks);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkoutSummary> list(UUID userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<WorkoutSession> found = sessions.findByUserIdOrderBySessionDateDescCreatedAtDesc(
                userId, PageRequest.of(Math.max(page, 0), safeSize));
        List<UUID> ids = found.getContent().stream().map(WorkoutSession::getId).toList();

        Map<UUID, Integer> setCounts = new HashMap<>();
        Map<UUID, Set<UUID>> exerciseIds = new HashMap<>();
        if (!ids.isEmpty()) {
            for (SessionSet set : sets.findBySessionIdIn(ids)) {
                setCounts.merge(set.getSessionId(), 1, Integer::sum);
                exerciseIds.computeIfAbsent(set.getSessionId(), k -> new LinkedHashSet<>()).add(set.getExerciseId());
            }
        }
        Map<UUID, int[]> arrows = new HashMap<>();
        if (!ids.isEmpty()) {
            for (ExerciseHistory history : histories.findBySessionIdIn(ids)) {
                int[] counts = arrows.computeIfAbsent(history.getSessionId(), k -> new int[3]);
                if ("UP".equals(history.getDeltaDirection())) {
                    counts[0]++;
                } else if ("DOWN".equals(history.getDeltaDirection())) {
                    counts[1]++;
                }
                if (history.isPr()) {
                    counts[2]++;
                }
            }
        }
        List<WorkoutSummary> items = found.getContent().stream().map(s -> {
            int[] counts = arrows.getOrDefault(s.getId(), new int[3]);
            return new WorkoutSummary(s.getId(), s.getName(), s.getSessionDate(), s.getDurationMinutes(),
                    s.getTotalVolumeKg(), exerciseIds.getOrDefault(s.getId(), Set.of()).size(),
                    setCounts.getOrDefault(s.getId(), 0), counts[0], counts[1], counts[2]);
        }).toList();
        return new PageResponse<>(items, found.getNumber(), found.getSize(), found.getTotalElements());
    }

    /** Last logged performance for each exercise, used as the "last time" hint. */
    @Transactional(readOnly = true)
    public List<PreviousPerformance> previous(UUID userId, Collection<UUID> exerciseIds) {
        List<PreviousPerformance> result = new ArrayList<>();
        for (UUID exerciseId : new LinkedHashSet<>(exerciseIds)) {
            List<ExerciseHistory> chain = histories.findByUserIdAndExerciseIdOrderBySessionDateAsc(userId, exerciseId);
            if (chain.isEmpty()) {
                continue;
            }
            ExerciseHistory last = chain.get(chain.size() - 1);
            List<SetResponse> lastSets = sets.findBySessionIdOrderBySequenceAsc(last.getSessionId()).stream()
                    .filter(s -> s.getExerciseId().equals(exerciseId))
                    .map(WorkoutService::toSetResponse)
                    .toList();
            result.add(new PreviousPerformance(exerciseId, last.getSessionDate(), last.getBestOneRm(), lastSets));
        }
        return result;
    }

    private WorkoutSession require(UUID userId, UUID sessionId) {
        return sessions.findByIdAndUserId(sessionId, userId).orElseThrow(() -> ApiException.notFound("Workout"));
    }

    private Map<UUID, Exercise> loadExercises(Collection<UUID> ids) {
        Map<UUID, Exercise> byId = new HashMap<>();
        exercises.findAllById(ids).forEach(e -> byId.put(e.getId(), e));
        return byId;
    }

    private static SetResponse toSetResponse(SessionSet set) {
        return new SetResponse(set.getSetNumber(), set.getWeightKg(), set.getReps(), set.getEstimatedOneRm(),
                set.getRestSeconds());
    }
}
