package com.repcheck.api.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A logged set together with the date of the session it belongs to. */
public record SetWithSession(UUID sessionId, LocalDate sessionDate, Instant createdAt,
                             Double weightKg, Integer reps, Integer cleanReps,
                             Double estimatedOneRm) {
}
