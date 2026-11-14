package com.repcheck.api.repository;

import com.repcheck.api.entity.ExerciseHistory;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseHistoryRepository extends JpaRepository<ExerciseHistory, UUID> {

    List<ExerciseHistory> findByUserIdAndExerciseIdOrderBySessionDateAsc(UUID userId, UUID exerciseId);

    List<ExerciseHistory> findByUserIdOrderBySessionDateAsc(UUID userId);

    List<ExerciseHistory> findBySessionIdIn(Collection<UUID> sessionIds);

    List<ExerciseHistory> findByUserIdAndSessionDateBetween(UUID userId, LocalDate from, LocalDate to);

    @Modifying
    @Query("delete from ExerciseHistory h where h.userId = :userId and h.exerciseId = :exerciseId")
    void deleteForExercise(@Param("userId") UUID userId, @Param("exerciseId") UUID exerciseId);
}
