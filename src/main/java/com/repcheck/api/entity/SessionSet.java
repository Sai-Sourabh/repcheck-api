package com.repcheck.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_sets")
@Getter
@Setter
@NoArgsConstructor
public class SessionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    /** Order of the set within the whole session. */
    @Column(name = "seq", nullable = false)
    private int sequence;

    /** Order of the set within its exercise. */
    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "weight_kg", nullable = false)
    private double weightKg;

    @Column(nullable = false)
    private int reps;

    @Column(name = "estimated_1rm", nullable = false)
    private double estimatedOneRm;

    @Column(name = "rest_seconds")
    private Integer restSeconds;
}
