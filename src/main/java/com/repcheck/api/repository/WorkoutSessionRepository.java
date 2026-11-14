package com.repcheck.api.repository;

import com.repcheck.api.entity.WorkoutSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    Page<WorkoutSession> findByUserIdOrderBySessionDateDescCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<WorkoutSession> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndSessionDateBetween(UUID userId, LocalDate from, LocalDate to);

    @Query("select new com.repcheck.api.repository.DailyVolume(w.sessionDate, sum(w.totalVolumeKg), count(w)) "
            + "from WorkoutSession w where w.userId = :userId and w.sessionDate >= :from "
            + "group by w.sessionDate order by w.sessionDate")
    List<DailyVolume> dailyVolumes(@Param("userId") UUID userId, @Param("from") LocalDate from);

    @Query("select min(w.sessionDate) from WorkoutSession w where w.userId = :userId")
    LocalDate firstSessionDate(@Param("userId") UUID userId);

    @Query("select max(w.sessionDate) from WorkoutSession w where w.userId = :userId")
    LocalDate lastSessionDate(@Param("userId") UUID userId);

    @Query("select distinct w.userId from WorkoutSession w")
    List<UUID> userIdsWithSessions();
}
