package com.repcheck.api.service;

import com.repcheck.api.dto.ActivityDtos.BodyWeightRequest;
import com.repcheck.api.dto.ActivityDtos.BodyWeightResponse;
import com.repcheck.api.entity.BodyWeightLog;
import com.repcheck.api.entity.User;
import com.repcheck.api.exception.ApiException;
import com.repcheck.api.repository.BodyWeightLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BodyWeightService {

    private final BodyWeightLogRepository logs;
    private final UserService userService;
    private final Clock clock;

    public BodyWeightService(BodyWeightLogRepository logs, UserService userService, Clock clock) {
        this.logs = logs;
        this.userService = userService;
        this.clock = clock;
    }

    /** One entry per day; logging again on the same day replaces it. */
    @Transactional
    public BodyWeightResponse log(UUID userId, BodyWeightRequest request) {
        LocalDate today = LocalDate.now(clock);
        LocalDate date = request.logDate() == null ? today : request.logDate();
        if (date.isAfter(today)) {
            throw ApiException.badRequest("logDate cannot be in the future");
        }
        BodyWeightLog log = logs.findByUserIdAndLogDate(userId, date).orElseGet(BodyWeightLog::new);
        log.setUserId(userId);
        log.setLogDate(date);
        log.setWeightKg(request.weightKg());
        logs.save(log);
        logs.flush();

        // The profile always shows the most recent weigh-in.
        User user = userService.require(userId);
        logs.findFirstByUserIdOrderByLogDateDesc(userId).ifPresent(latest -> {
            user.setBodyWeightKg(latest.getWeightKg());
            user.setUpdatedAt(Instant.now());
        });
        return new BodyWeightResponse(log.getLogDate(), log.getWeightKg());
    }

    @Transactional(readOnly = true)
    public List<BodyWeightResponse> list(UUID userId, int days) {
        int window = Math.min(Math.max(days, 1), 730);
        return logs.findByUserIdAndLogDateGreaterThanEqualOrderByLogDateAsc(
                        userId, LocalDate.now(clock).minusDays(window - 1L)).stream()
                .map(l -> new BodyWeightResponse(l.getLogDate(), l.getWeightKg()))
                .toList();
    }
}
