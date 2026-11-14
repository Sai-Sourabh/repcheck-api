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

/** Weekly streak. One row per user per type (WORKOUT or STEPS). */
@Entity
@Table(name = "streaks")
@Getter
@Setter
@NoArgsConstructor
public class Streak {

    public static final String WORKOUT = "WORKOUT";
    public static final String STEPS = "STEPS";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "streak_type", nullable = false)
    private String streakType;

    /** Completed weeks in a row that hit the goal. */
    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    /** Monday of the most recent completed week that has been scored. */
    @Column(name = "last_evaluated_week", nullable = false)
    private LocalDate lastEvaluatedWeek;

    @Column(name = "freeze_tokens", nullable = false)
    private int freezeTokens = 1;
}
