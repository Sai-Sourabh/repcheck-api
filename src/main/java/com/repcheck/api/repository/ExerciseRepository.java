package com.repcheck.api.repository;

import com.repcheck.api.entity.Exercise;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    /** Built-in exercises plus the user's own custom ones. */
    @Query("select e from Exercise e where e.custom = false or e.createdBy = :userId order by e.name")
    List<Exercise> findVisibleTo(@Param("userId") UUID userId);
}
