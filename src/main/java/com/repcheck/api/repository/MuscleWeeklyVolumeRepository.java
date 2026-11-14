package com.repcheck.api.repository;

import com.repcheck.api.entity.MuscleWeeklyVolume;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MuscleWeeklyVolumeRepository extends JpaRepository<MuscleWeeklyVolume, UUID> {

    @Modifying
    @Query("delete from MuscleWeeklyVolume m where m.userId = :userId and m.weekStart = :weekStart")
    void deleteWeek(@Param("userId") UUID userId, @Param("weekStart") LocalDate weekStart);
}
