package com.repcheck.api.repository;

import com.repcheck.api.entity.DailyLoad;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyLoadRepository extends JpaRepository<DailyLoad, UUID> {

    List<DailyLoad> findByUserIdAndLoadDateGreaterThanEqualOrderByLoadDateAsc(UUID userId, LocalDate from);

    Optional<DailyLoad> findByUserIdAndLoadDate(UUID userId, LocalDate loadDate);

    Optional<DailyLoad> findFirstByUserIdAndLoadDateLessThanEqualOrderByLoadDateDesc(UUID userId, LocalDate date);

    @Modifying
    @Query("delete from DailyLoad d where d.userId = :userId and d.loadDate >= :from")
    void deleteFrom(@Param("userId") UUID userId, @Param("from") LocalDate from);

    @Modifying
    @Query("delete from DailyLoad d where d.userId = :userId")
    void deleteAllForUser(@Param("userId") UUID userId);
}
