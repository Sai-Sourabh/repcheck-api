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
@Table(name = "cardio_sessions")
@Getter
@Setter
@NoArgsConstructor
public class CardioSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    /** WALK, RUN, CYCLE, SWIM, OTHER. */
    @Column(name = "activity_type", nullable = false)
    private String activityType;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    private Integer steps;

    @Column(name = "avg_heart_rate")
    private Integer avgHeartRate;
}
