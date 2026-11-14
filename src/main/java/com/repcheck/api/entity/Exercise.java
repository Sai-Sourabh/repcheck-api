package com.repcheck.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "muscle_group", nullable = false)
    private String muscleGroup;

    /** Comma separated muscle groups, e.g. "TRICEPS,SHOULDERS". */
    @Column(name = "secondary_muscles")
    private String secondaryMuscles;

    @Column(nullable = false)
    private String equipment;

    /** PUSH, PULL, LEGS or CORE. */
    @Column(nullable = false)
    private String category;

    @Column(name = "is_custom", nullable = false)
    private boolean custom;

    @Column(name = "created_by")
    private UUID createdBy;

    public List<String> secondaryMuscleList() {
        if (secondaryMuscles == null || secondaryMuscles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(secondaryMuscles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
