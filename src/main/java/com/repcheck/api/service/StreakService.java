package com.repcheck.api.service;

import com.repcheck.api.domain.Weeks;
import com.repcheck.api.dto.ActivityDtos.StreakResponse;
import com.repcheck.api.dto.ActivityDtos.StreaksResponse;
import com.repcheck.api.entity.CardioSession;
import com.repcheck.api.entity.Streak;
import com.repcheck.api.entity.User;
import com.repcheck.api.repository.CardioSessionRepository;
import com.repcheck.api.repository.StreakRepository;
import com.repcheck.api.repository.WorkoutSessionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Weekly streaks. A week counts when the goal is hit: workouts per week for
 * WORKOUT, total steps for STEPS. A missed week burns a freeze token instead
 * of resetting the streak, and every 4th week in a row earns a token back.
 *
 * Completed weeks are scored lazily the next time the streak is read, so the
 * result does not depend on a scheduled job having run.
 */
@Service
public class StreakService {

    static final int MAX_FREEZE_TOKENS = 2;
    static final int WEEKS_PER_TOKEN = 4;
    private static final int MAX_LOOKBACK_WEEKS = 104;

    private final StreakRepository streaks;
    private final WorkoutSessionRepository sessions;
    private final CardioSessionRepository cardio;
    private final UserService userService;
    private final Clock clock;

    public StreakService(StreakRepository streaks, WorkoutSessionRepository sessions,
                         CardioSessionRepository cardio, UserService userService, Clock clock) {
        this.streaks = streaks;
        this.sessions = sessions;
        this.cardio = cardio;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional
    public StreaksResponse get(UUID userId) {
        User user = userService.require(userId);
        LocalDate thisWeek = Weeks.startOf(LocalDate.now(clock));
        return new StreaksResponse(evaluate(user, Streak.WORKOUT, thisWeek), evaluate(user, Streak.STEPS, thisWeek));
    }

    private StreakResponse evaluate(User user, String type, LocalDate thisWeek) {
        Streak streak = streaks.findByUserIdAndStreakType(user.getId(), type)
                .orElseGet(() -> create(user, type, thisWeek));
        int goal = goal(user, type);

        LocalDate week = streak.getLastEvaluatedWeek().plusWeeks(1);
        while (week.isBefore(thisWeek)) {
            score(streak, progress(user.getId(), type, week) >= goal);
            streak.setLastEvaluatedWeek(week);
            week = week.plusWeeks(1);
        }
        streaks.save(streak);

        int progress = progress(user.getId(), type, thisWeek);
        boolean met = progress >= goal;
        // The week in progress only ever adds to the streak; it cannot break it yet.
        int current = streak.getCurrentStreak() + (met ? 1 : 0);
        return new StreakResponse(type, current, Math.max(streak.getLongestStreak(), current),
                streak.getFreezeTokens(), progress, goal, met);
    }

    /** Applies one finished week to the streak. */
    static void score(Streak streak, boolean goalMet) {
        if (goalMet) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            streak.setLongestStreak(Math.max(streak.getLongestStreak(), streak.getCurrentStreak()));
            if (streak.getCurrentStreak() % WEEKS_PER_TOKEN == 0 && streak.getFreezeTokens() < MAX_FREEZE_TOKENS) {
                streak.setFreezeTokens(streak.getFreezeTokens() + 1);
            }
        } else if (streak.getCurrentStreak() > 0 && streak.getFreezeTokens() > 0) {
            streak.setFreezeTokens(streak.getFreezeTokens() - 1);
        } else {
            streak.setCurrentStreak(0);
        }
    }

    private Streak create(User user, String type, LocalDate thisWeek) {
        // Start scoring from the first week with any activity so back-filled history counts.
        LocalDate firstActivity = Streak.WORKOUT.equals(type)
                ? sessions.firstSessionDate(user.getId())
                : cardio.findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(
                        user.getId(), thisWeek.minusWeeks(MAX_LOOKBACK_WEEKS), thisWeek).stream()
                        .map(CardioSession::getSessionDate).min(LocalDate::compareTo).orElse(null);
        LocalDate firstWeek = firstActivity == null ? thisWeek : Weeks.startOf(firstActivity);
        if (firstWeek.isBefore(thisWeek.minusWeeks(MAX_LOOKBACK_WEEKS))) {
            firstWeek = thisWeek.minusWeeks(MAX_LOOKBACK_WEEKS);
        }
        Streak streak = new Streak();
        streak.setUserId(user.getId());
        streak.setStreakType(type);
        streak.setLastEvaluatedWeek(firstWeek.minusWeeks(1));
        streak.setLastActivityDate(firstActivity);
        return streak;
    }

    private int goal(User user, String type) {
        return Streak.WORKOUT.equals(type) ? user.getWeeklyWorkoutGoal() : user.getWeeklyStepsGoal();
    }

    private int progress(UUID userId, String type, LocalDate weekStart) {
        LocalDate weekEnd = Weeks.endOf(weekStart);
        if (Streak.WORKOUT.equals(type)) {
            return (int) sessions.countByUserIdAndSessionDateBetween(userId, weekStart, weekEnd);
        }
        return cardio.findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(userId, weekStart, weekEnd).stream()
                .mapToInt(c -> c.getSteps() == null ? 0 : c.getSteps())
                .sum();
    }
}
