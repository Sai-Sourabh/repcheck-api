package com.repcheck.api.service;

import com.repcheck.api.domain.VolumeLandmarks;
import com.repcheck.api.dto.ExerciseDtos.CreateExerciseRequest;
import com.repcheck.api.dto.ExerciseDtos.ExerciseResponse;
import com.repcheck.api.entity.Exercise;
import com.repcheck.api.exception.ApiException;
import com.repcheck.api.repository.ExerciseRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseService {

    private static final Set<String> CATEGORIES = Set.of("PUSH", "PULL", "LEGS", "CORE");

    private final ExerciseRepository exercises;

    public ExerciseService(ExerciseRepository exercises) {
        this.exercises = exercises;
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> list(UUID userId, String muscleGroup, String query) {
        String muscle = muscleGroup == null || muscleGroup.isBlank() ? null : muscleGroup.trim().toUpperCase(Locale.ROOT);
        String q = query == null || query.isBlank() ? null : query.trim().toLowerCase(Locale.ROOT);
        return exercises.findVisibleTo(userId).stream()
                .filter(e -> muscle == null || e.getMuscleGroup().equals(muscle))
                .filter(e -> q == null || e.getName().toLowerCase(Locale.ROOT).contains(q))
                .map(ExerciseService::toResponse)
                .toList();
    }

    @Transactional
    public ExerciseResponse create(UUID userId, CreateExerciseRequest request) {
        String muscle = normalizeMuscle(request.muscleGroup());
        String category = request.category().trim().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(category)) {
            throw ApiException.badRequest("category must be one of " + CATEGORIES);
        }
        List<String> secondary = request.secondaryMuscles() == null ? List.of()
                : request.secondaryMuscles().stream().map(this::normalizeMuscle)
                        .filter(m -> !m.equals(muscle)).distinct().toList();

        Exercise exercise = new Exercise();
        exercise.setName(request.name().trim());
        exercise.setMuscleGroup(muscle);
        exercise.setSecondaryMuscles(secondary.isEmpty() ? null : String.join(",", secondary));
        exercise.setEquipment(request.equipment().trim().toUpperCase(Locale.ROOT));
        exercise.setCategory(category);
        exercise.setCustom(true);
        exercise.setCreatedBy(userId);
        return toResponse(exercises.save(exercise));
    }

    @Transactional
    public void delete(UUID userId, UUID exerciseId) {
        Exercise exercise = exercises.findById(exerciseId)
                .filter(e -> e.isCustom() && userId.equals(e.getCreatedBy()))
                .orElseThrow(() -> ApiException.notFound("Custom exercise"));
        try {
            exercises.delete(exercise);
            exercises.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("This exercise has logged sets and cannot be deleted");
        }
    }

    /** Loads the given exercises, failing if any is missing or belongs to another user. */
    @Transactional(readOnly = true)
    public Map<UUID, Exercise> requireVisible(UUID userId, Collection<UUID> ids) {
        Map<UUID, Exercise> byId = new HashMap<>();
        for (Exercise exercise : exercises.findAllById(ids)) {
            if (!exercise.isCustom() || userId.equals(exercise.getCreatedBy())) {
                byId.put(exercise.getId(), exercise);
            }
        }
        for (UUID id : ids) {
            if (!byId.containsKey(id)) {
                throw ApiException.badRequest("Unknown exercise: " + id);
            }
        }
        return byId;
    }

    private String normalizeMuscle(String value) {
        String muscle = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!VolumeLandmarks.isKnown(muscle)) {
            throw ApiException.badRequest("muscleGroup must be one of " + VolumeLandmarks.muscleGroups());
        }
        return muscle;
    }

    public static ExerciseResponse toResponse(Exercise e) {
        return new ExerciseResponse(e.getId(), e.getName(), e.getMuscleGroup(), e.secondaryMuscleList(),
                e.getEquipment(), e.getCategory(), e.isCustom());
    }
}
