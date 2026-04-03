package com.econference.auth.adapter.out.persistence;

import com.econference.auth.domain.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionJpaRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByTokenHashAndInvalidatedAtIsNull(String tokenHash);

    @Modifying
    @Query("UPDATE UserSession s SET s.invalidatedAt = :now WHERE s.user.id = :userId AND s.invalidatedAt IS NULL")
    int invalidateAllByUserId(UUID userId, Instant now);
}
