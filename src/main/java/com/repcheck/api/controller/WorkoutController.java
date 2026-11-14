package com.repcheck.api.controller;

import com.repcheck.api.dto.WorkoutDtos.CreateWorkoutRequest;
import com.repcheck.api.dto.WorkoutDtos.PageResponse;
import com.repcheck.api.dto.WorkoutDtos.PreviousPerformance;
import com.repcheck.api.dto.WorkoutDtos.WorkoutDetail;
import com.repcheck.api.dto.WorkoutDtos.WorkoutSummary;
import com.repcheck.api.security.AuthUser;
import com.repcheck.api.service.WorkoutService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/workouts")
@Tag(name = "Workouts")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutDetail create(@AuthenticationPrincipal AuthUser user,
                                @Valid @RequestBody CreateWorkoutRequest request) {
        return workoutService.create(user.id(), request);
    }

    @GetMapping
    public PageResponse<WorkoutSummary> list(@AuthenticationPrincipal AuthUser user,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return workoutService.list(user.id(), page, size);
    }

    /** What the user did last time on each of the given exercises. */
    @GetMapping("/previous")
    public List<PreviousPerformance> previous(@AuthenticationPrincipal AuthUser user,
                                              @RequestParam List<UUID> exerciseIds) {
        return workoutService.previous(user.id(), exerciseIds);
    }

    @GetMapping("/{id}")
    public WorkoutDetail get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return workoutService.detail(user.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        workoutService.delete(user.id(), id);
    }
}
