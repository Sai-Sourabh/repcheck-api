package com.repcheck.api.job;

import com.repcheck.api.domain.Weeks;
import com.repcheck.api.repository.WorkoutSessionRepository;
import com.repcheck.api.service.AnalyticsService;
import com.repcheck.api.service.StreakService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background refresh. Everything here is also computed on demand, so a missed
 * run (for example a sleeping free-tier host) never leaves the data wrong.
 */
@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final WorkoutSessionRepository sessions;
    private final AnalyticsService analyticsService;
    private final StreakService streakService;
    private final Clock clock;

    public ScheduledJobs(WorkoutSessionRepository sessions, AnalyticsService analyticsService,
                         StreakService streakService, Clock clock) {
        this.sessions = sessions;
        this.analyticsService = analyticsService;
        this.streakService = streakService;
        this.clock = clock;
    }

    /** Rolls ATL / CTL / TSB forward a day, so rest days show recovery. */
    @Scheduled(cron = "0 5 0 * * *", zone = "${app.timezone}")
    public void dailyLoadJob() {
        LocalDate today = LocalDate.now(clock);
        forEachUser("daily load", userId -> analyticsService.recomputeDailyLoad(userId, today));
    }

    /** Sunday night: lock in the week's volume and next week's recommendation. */
    @Scheduled(cron = "0 30 23 * * SUN", zone = "${app.timezone}")
    public void weeklyVolumeJob() {
        LocalDate weekStart = Weeks.startOf(LocalDate.now(clock));
        forEachUser("weekly volume", userId -> analyticsService.persistWeek(userId, weekStart));
    }

    /** Monday morning: score the week that just ended. */
    @Scheduled(cron = "0 10 0 * * MON", zone = "${app.timezone}")
    public void streakJob() {
        forEachUser("streaks", streakService::get);
    }

    private void forEachUser(String name, java.util.function.Consumer<UUID> task) {
        int done = 0;
        for (UUID userId : sessions.userIdsWithSessions()) {
            try {
                task.accept(userId);
                done++;
            } catch (RuntimeException ex) {
                log.warn("{} job failed for user {}", name, userId, ex);
            }
        }
        log.info("{} job finished for {} users", name, done);
    }
}
