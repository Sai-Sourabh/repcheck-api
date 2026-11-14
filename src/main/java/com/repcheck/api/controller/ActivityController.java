package com.repcheck.api.controller;

import com.repcheck.api.dto.ActivityDtos.BodyWeightRequest;
import com.repcheck.api.dto.ActivityDtos.BodyWeightResponse;
import com.repcheck.api.dto.ActivityDtos.CardioRequest;
import com.repcheck.api.dto.ActivityDtos.CardioResponse;
import com.repcheck.api.dto.ActivityDtos.StepsWeekResponse;
import com.repcheck.api.dto.ActivityDtos.StreaksResponse;
import com.repcheck.api.security.AuthUser;
import com.repcheck.api.service.BodyWeightService;
import com.repcheck.api.service.CardioService;
import com.repcheck.api.service.StreakService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Streaks, cardio and body weight")
public class ActivityController {

    private final StreakService streakService;
    private final CardioService cardioService;
    private final BodyWeightService bodyWeightService;

    public ActivityController(StreakService streakService, CardioService cardioService,
                              BodyWeightService bodyWeightService) {
        this.streakService = streakService;
        this.cardioService = cardioService;
        this.bodyWeightService = bodyWeightService;
    }

    @GetMapping("/streaks")
    public StreaksResponse streaks(@AuthenticationPrincipal AuthUser user) {
        return streakService.get(user.id());
    }

    @PostMapping("/cardio")
    @ResponseStatus(HttpStatus.CREATED)
    public CardioResponse logCardio(@AuthenticationPrincipal AuthUser user,
                                    @Valid @RequestBody CardioRequest request) {
        return cardioService.create(user.id(), request);
    }

    @GetMapping("/cardio")
    public List<CardioResponse> cardio(@AuthenticationPrincipal AuthUser user,
                                       @RequestParam(defaultValue = "30") int days) {
        return cardioService.list(user.id(), days);
    }

    @DeleteMapping("/cardio/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCardio(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        cardioService.delete(user.id(), id);
    }

    /** Steps per day against the weekly goal, for the week containing {@code date}. */
    @GetMapping("/cardio/steps")
    public StepsWeekResponse steps(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return cardioService.stepsWeek(user.id(), date);
    }

    @PostMapping("/body-weight")
    public BodyWeightResponse logBodyWeight(@AuthenticationPrincipal AuthUser user,
                                            @Valid @RequestBody BodyWeightRequest request) {
        return bodyWeightService.log(user.id(), request);
    }

    @GetMapping("/body-weight")
    public List<BodyWeightResponse> bodyWeight(@AuthenticationPrincipal AuthUser user,
                                               @RequestParam(defaultValue = "180") int days) {
        return bodyWeightService.list(user.id(), days);
    }
}
