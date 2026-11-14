package com.repcheck.api.repository;

import com.repcheck.api.entity.Streak;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreakRepository extends JpaRepository<Streak, UUID> {

    Optional<Streak> findByUserIdAndStreakType(UUID userId, String streakType);
}
