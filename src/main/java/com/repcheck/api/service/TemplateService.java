package com.repcheck.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repcheck.api.dto.TemplateDtos.DayExerciseResponse;
import com.repcheck.api.dto.TemplateDtos.DayRequest;
import com.repcheck.api.dto.TemplateDtos.DayResponse;
import com.repcheck.api.dto.TemplateDtos.TemplateRequest;
import com.repcheck.api.dto.TemplateDtos.TemplateResponse;
import com.repcheck.api.entity.Exercise;
import com.repcheck.api.entity.TemplateDay;
import com.repcheck.api.entity.TemplateDay.TemplateExercise;
import com.repcheck.api.entity.WorkoutTemplate;
import com.repcheck.api.exception.ApiException;
import com.repcheck.api.repository.ExerciseRepository;
import com.repcheck.api.repository.WorkoutTemplateRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {

    private static final TypeReference<List<TemplateDay>> DAYS_TYPE = new TypeReference<>() {
    };

    private final WorkoutTemplateRepository templates;
    private final ExerciseRepository exercises;
    private final ExerciseService exerciseService;
    private final ObjectMapper objectMapper;

    public TemplateService(WorkoutTemplateRepository templates, ExerciseRepository exercises,
                           ExerciseService exerciseService, ObjectMapper objectMapper) {
        this.templates = templates;
        this.exercises = exercises;
        this.exerciseService = exerciseService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> list(UUID userId) {
        List<WorkoutTemplate> found = templates.findByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, Exercise> names = exerciseNames(found);
        return found.stream().map(t -> toResponse(t, names)).toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse get(UUID userId, UUID id) {
        WorkoutTemplate template = require(userId, id);
        return toResponse(template, exerciseNames(List.of(template)));
    }

    @Transactional
    public TemplateResponse create(UUID userId, TemplateRequest request) {
        WorkoutTemplate template = new WorkoutTemplate();
        template.setUserId(userId);
        apply(userId, template, request);
        templates.save(template);
        return toResponse(template, exerciseNames(List.of(template)));
    }

    @Transactional
    public TemplateResponse update(UUID userId, UUID id, TemplateRequest request) {
        WorkoutTemplate template = require(userId, id);
        apply(userId, template, request);
        return toResponse(template, exerciseNames(List.of(template)));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        templates.delete(require(userId, id));
    }

    private WorkoutTemplate require(UUID userId, UUID id) {
        return templates.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Template"));
    }

    private void apply(UUID userId, WorkoutTemplate template, TemplateRequest request) {
        Set<UUID> ids = new LinkedHashSet<>();
        List<TemplateDay> days = new ArrayList<>();
        for (DayRequest day : request.days()) {
            List<TemplateExercise> items = day.exercises() == null ? List.of()
                    : day.exercises().stream()
                            .map(e -> new TemplateExercise(e.exerciseId(), e.targetSets(), e.targetReps()))
                            .toList();
            items.forEach(e -> ids.add(e.exerciseId()));
            days.add(new TemplateDay(day.name().trim(), items));
        }
        exerciseService.requireVisible(userId, ids);
        template.setName(request.name().trim());
        template.setSplitType(request.splitType() == null || request.splitType().isBlank()
                ? null : request.splitType().trim());
        template.setDaysJson(writeDays(days));
    }

    private List<TemplateDay> readDays(WorkoutTemplate template) {
        try {
            return objectMapper.readValue(template.getDaysJson(), DAYS_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Corrupt template " + template.getId(), ex);
        }
    }

    private String writeDays(List<TemplateDay> days) {
        try {
            return objectMapper.writeValueAsString(days);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize template days", ex);
        }
    }

    private Map<UUID, Exercise> exerciseNames(List<WorkoutTemplate> found) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (WorkoutTemplate template : found) {
            for (TemplateDay day : readDays(template)) {
                day.exercises().forEach(e -> ids.add(e.exerciseId()));
            }
        }
        Map<UUID, Exercise> byId = new HashMap<>();
        exercises.findAllById(ids).forEach(e -> byId.put(e.getId(), e));
        return byId;
    }

    private TemplateResponse toResponse(WorkoutTemplate template, Map<UUID, Exercise> names) {
        List<DayResponse> days = readDays(template).stream()
                .map(day -> new DayResponse(day.name(), day.exercises().stream()
                        .filter(e -> names.containsKey(e.exerciseId()))
                        .map(e -> {
                            Exercise exercise = names.get(e.exerciseId());
                            return new DayExerciseResponse(e.exerciseId(), exercise.getName(),
                                    exercise.getMuscleGroup(), e.targetSets(), e.targetReps());
                        })
                        .toList()))
                .toList();
        return new TemplateResponse(template.getId(), template.getName(), template.getSplitType(), days,
                template.getCreatedAt());
    }
}
