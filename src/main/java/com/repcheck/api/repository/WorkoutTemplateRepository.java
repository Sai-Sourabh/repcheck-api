package com.repcheck.api.repository;

import com.repcheck.api.entity.WorkoutTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, UUID> {

    List<WorkoutTemplate> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<WorkoutTemplate> findByIdAndUserId(UUID id, UUID userId);
}
