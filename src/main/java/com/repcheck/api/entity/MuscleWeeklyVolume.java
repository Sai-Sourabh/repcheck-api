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
@Table(name = "muscle_weekly_volume")
@Getter
@Setter
@NoArgsConstructor
public class MuscleWeeklyVolume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "muscle_group", nullable = false)
    private String muscleGroup;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    /** Effective sets: 1 per set as primary mover, 0.5 per set as secondary. */
    @Column(name = "total_sets", nullable = false)
    private double totalSets;

    @Column(name = "total_volume_kg", nullable = false)
    private double totalVolumeKg;

    /** INCREASE, MAINTAIN or DELOAD. */
    @Column(nullable = false)
    private String recommendation;

    private String reason;

    @Column(name = "suggested_sets")
    private Integer suggestedSets;
}
