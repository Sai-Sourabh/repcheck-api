package com.repcheck.api.repository;

import com.repcheck.api.entity.CardioSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardioSessionRepository extends JpaRepository<CardioSession, UUID> {

    List<CardioSession> findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(UUID userId, LocalDate from,
                                                                               LocalDate to);

    Optional<CardioSession> findByIdAndUserId(UUID id, UUID userId);
}
