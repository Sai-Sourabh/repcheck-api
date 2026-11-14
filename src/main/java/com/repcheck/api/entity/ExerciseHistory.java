package com.repcheck.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exercise_history")
@Getter
@Setter
@NoArgsConstructor
public class ExerciseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "best_1rm", nullable = false)
    private double bestOneRm;

    @Column(name = "best_weight_kg", nullable = false)
    private double bestWeightKg;

    @Column(name = "best_reps", nullable = false)
    private int bestReps;

    @Column(name = "total_volume_kg", nullable = false)
    private double totalVolumeKg;

    @Column(name = "total_sets", nullable = false)
    private int totalSets;

    /** Null for the first ever session of an exercise. */
    @Column(name = "delta_pct")
    private Double deltaPct;

    /** UP, DOWN, SAME or NEW. */
    @Column(name = "delta_direction", nullable = false)
    private String deltaDirection;

    @Column(name = "is_pr", nullable = false)
    private boolean pr;
}
