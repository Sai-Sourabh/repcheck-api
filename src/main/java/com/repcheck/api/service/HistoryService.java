package com.repcheck.api.service;

import com.repcheck.api.domain.OneRMCalculator;
import com.repcheck.api.domain.OneRMCalculator.Delta;
import com.repcheck.api.entity.ExerciseHistory;
import com.repcheck.api.repository.ExerciseHistoryRepository;
import com.repcheck.api.repository.SessionSetRepository;
import com.repcheck.api.repository.SetWithSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains exercise_history: one pre-computed row per exercise per session
 * holding the best estimated 1RM and its change against the session before.
 */
@Service
public class HistoryService {

    private final SessionSetRepository sets;
    private final ExerciseHistoryRepository histories;

    public HistoryService(SessionSetRepository sets, ExerciseHistoryRepository histories) {
        this.sets = sets;
        this.histories = histories;
    }

    /**
     * Rebuilds the whole chain for one exercise. Rebuilding (instead of
     * appending) keeps every arrow correct when a session is back-dated or deleted.
     */
    @Transactional
    public void rebuild(UUID userId, UUID exerciseId) {
        Map<UUID, List<SetWithSession>> bySession = new LinkedHashMap<>();
        for (SetWithSession set : sets.findForExercise(userId, exerciseId)) {
            bySession.computeIfAbsent(set.sessionId(), k -> new ArrayList<>()).add(set);
        }
        histories.deleteForExercise(userId, exerciseId);

        List<ExerciseHistory> rows = new ArrayList<>();
        Double previousMetric = null;
        double bestEver = 0;
        for (Map.Entry<UUID, List<SetWithSession>> entry : bySession.entrySet()) {
            List<SetWithSession> sessionSets = entry.getValue();
            SetWithSession best = sessionSets.get(0);
            double volume = 0;
            for (SetWithSession set : sessionSets) {
                volume += set.weightKg() * set.reps();
                if (isBetter(set, best)) {
                    best = set;
                }
            }
            // Bodyweight lifts have no load, so progress is tracked on reps instead.
            double metric = best.estimatedOneRm() > 0 ? best.estimatedOneRm() : best.reps();
            Delta delta = OneRMCalculator.delta(previousMetric, metric);

            ExerciseHistory row = new ExerciseHistory();
            row.setUserId(userId);
            row.setExerciseId(exerciseId);
            row.setSessionId(entry.getKey());
            row.setSessionDate(best.sessionDate());
            row.setBestOneRm(best.estimatedOneRm());
            row.setBestWeightKg(best.weightKg());
            row.setBestReps(best.reps());
            row.setTotalVolumeKg(OneRMCalculator.round2(volume));
            row.setTotalSets(sessionSets.size());
            row.setDeltaPct(delta.pct());
            row.setDeltaDirection(delta.direction().name());
            row.setPr(previousMetric != null && metric > bestEver);
            rows.add(row);

            previousMetric = metric;
            bestEver = Math.max(bestEver, metric);
        }
        histories.saveAll(rows);
    }

    private static boolean isBetter(SetWithSession candidate, SetWithSession best) {
        int byOneRm = Double.compare(candidate.estimatedOneRm(), best.estimatedOneRm());
        if (byOneRm != 0) {
            return byOneRm > 0;
        }
        return candidate.reps() > best.reps();
    }
}
