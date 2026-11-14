package com.repcheck.api.repository;

import com.repcheck.api.entity.SessionSet;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionSetRepository extends JpaRepository<SessionSet, UUID> {

    List<SessionSet> findBySessionIdOrderBySequenceAsc(UUID sessionId);

    List<SessionSet> findBySessionIdIn(Collection<UUID> sessionIds);

    @Modifying
    @Query("delete from SessionSet s where s.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") UUID sessionId);

    /** Every set the user has logged for one exercise, oldest session first. */
    @Query("select new com.repcheck.api.repository.SetWithSession("
            + "s.sessionId, w.sessionDate, w.createdAt, s.weightKg, s.reps, s.estimatedOneRm) "
            + "from SessionSet s join WorkoutSession w on w.id = s.sessionId "
            + "where w.userId = :userId and s.exerciseId = :exerciseId "
            + "order by w.sessionDate asc, w.createdAt asc, s.setNumber asc")
    List<SetWithSession> findForExercise(@Param("userId") UUID userId, @Param("exerciseId") UUID exerciseId);

    @Query("select new com.repcheck.api.repository.SetWithExercise("
            + "e.muscleGroup, e.secondaryMuscles, e.category, s.weightKg, s.reps) "
            + "from SessionSet s join WorkoutSession w on w.id = s.sessionId "
            + "join Exercise e on e.id = s.exerciseId "
            + "where w.userId = :userId and w.sessionDate between :from and :to")
    List<SetWithExercise> findWithExercise(@Param("userId") UUID userId, @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);
}
