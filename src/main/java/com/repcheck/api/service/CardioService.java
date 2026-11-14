package com.repcheck.api.service;

import com.repcheck.api.domain.OneRMCalculator;
import com.repcheck.api.domain.Weeks;
import com.repcheck.api.dto.ActivityDtos.CardioRequest;
import com.repcheck.api.dto.ActivityDtos.CardioResponse;
import com.repcheck.api.dto.ActivityDtos.DaySteps;
import com.repcheck.api.dto.ActivityDtos.StepsWeekResponse;
import com.repcheck.api.entity.CardioSession;
import com.repcheck.api.entity.User;
import com.repcheck.api.exception.ApiException;
import com.repcheck.api.repository.CardioSessionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardioService {

    private static final Set<String> ACTIVITIES = Set.of("WALK", "RUN", "CYCLE", "SWIM", "OTHER");

    private final CardioSessionRepository cardio;
    private final UserService userService;
    private final Clock clock;

    public CardioService(CardioSessionRepository cardio, UserService userService, Clock clock) {
        this.cardio = cardio;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional
    public CardioResponse create(UUID userId, CardioRequest request) {
        LocalDate today = LocalDate.now(clock);
        LocalDate date = request.sessionDate() == null ? today : request.sessionDate();
        if (date.isAfter(today)) {
            throw ApiException.badRequest("sessionDate cannot be in the future");
        }
        String activity = request.activityType().trim().toUpperCase(Locale.ROOT);
        if (!ACTIVITIES.contains(activity)) {
            throw ApiException.badRequest("activityType must be one of " + ACTIVITIES);
        }
        if (request.steps() == null && request.distanceKm() == null && request.durationMinutes() == null) {
            throw ApiException.badRequest("Provide steps, distanceKm or durationMinutes");
        }
        CardioSession session = new CardioSession();
        session.setUserId(userId);
        session.setSessionDate(date);
        session.setActivityType(activity);
        session.setDistanceKm(request.distanceKm());
        session.setDurationMinutes(request.durationMinutes());
        session.setSteps(request.steps());
        session.setAvgHeartRate(request.avgHeartRate());
        return toResponse(cardio.save(session));
    }

    @Transactional(readOnly = true)
    public List<CardioResponse> list(UUID userId, int days) {
        LocalDate today = LocalDate.now(clock);
        int window = Math.min(Math.max(days, 1), 365);
        return cardio.findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(
                        userId, today.minusDays(window - 1L), today).stream()
                .map(CardioService::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        cardio.delete(cardio.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Cardio session")));
    }

    @Transactional(readOnly = true)
    public StepsWeekResponse stepsWeek(UUID userId, LocalDate anyDayInWeek) {
        User user = userService.require(userId);
        LocalDate weekStart = Weeks.startOf(anyDayInWeek == null ? LocalDate.now(clock) : anyDayInWeek);
        Map<LocalDate, Integer> byDay = new HashMap<>();
        for (CardioSession session : cardio.findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(
                userId, weekStart, Weeks.endOf(weekStart))) {
            if (session.getSteps() != null) {
                byDay.merge(session.getSessionDate(), session.getSteps(), Integer::sum);
            }
        }
        List<DaySteps> days = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            int steps = byDay.getOrDefault(day, 0);
            total += steps;
            days.add(new DaySteps(day, steps));
        }
        return new StepsWeekResponse(weekStart, user.getWeeklyStepsGoal(), total, days);
    }

    private static CardioResponse toResponse(CardioSession s) {
        Double pace = null;
        if (s.getDistanceKm() != null && s.getDistanceKm() > 0 && s.getDurationMinutes() != null
                && s.getDurationMinutes() > 0) {
            pace = OneRMCalculator.round2(s.getDurationMinutes() / s.getDistanceKm());
        }
        return new CardioResponse(s.getId(), s.getSessionDate(), s.getActivityType(), s.getDistanceKm(),
                s.getDurationMinutes(), s.getSteps(), s.getAvgHeartRate(), pace);
    }
}
