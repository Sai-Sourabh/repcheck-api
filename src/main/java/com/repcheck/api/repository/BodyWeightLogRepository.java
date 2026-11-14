package com.repcheck.api.repository;

import com.repcheck.api.entity.BodyWeightLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BodyWeightLogRepository extends JpaRepository<BodyWeightLog, UUID> {

    Optional<BodyWeightLog> findByUserIdAndLogDate(UUID userId, LocalDate logDate);

    Optional<BodyWeightLog> findFirstByUserIdOrderByLogDateDesc(UUID userId);

    List<BodyWeightLog> findByUserIdAndLogDateGreaterThanEqualOrderByLogDateAsc(UUID userId, LocalDate from);
}
