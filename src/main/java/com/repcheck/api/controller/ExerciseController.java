package com.repcheck.api.controller;

import com.repcheck.api.dto.ExerciseDtos.CreateExerciseRequest;
import com.repcheck.api.dto.ExerciseDtos.ExerciseResponse;
import com.repcheck.api.security.AuthUser;
import com.repcheck.api.service.ExerciseService;
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
@RequestMapping("/api/exercises")
@Tag(name = "Exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public List<ExerciseResponse> list(@AuthenticationPrincipal AuthUser user,
                                       @RequestParam(required = false) String muscle,
                                       @RequestParam(required = false) String q) {
        return exerciseService.list(user.id(), muscle, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseResponse create(@AuthenticationPrincipal AuthUser user,
                                   @Valid @RequestBody CreateExerciseRequest request) {
        return exerciseService.create(user.id(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        exerciseService.delete(user.id(), id);
    }
}
