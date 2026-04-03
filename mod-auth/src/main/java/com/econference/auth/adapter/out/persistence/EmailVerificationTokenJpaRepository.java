package com.econference.auth.adapter.out.persistence;

import com.econference.auth.domain.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByToken(String token);

    List<EmailVerificationToken> findByUserIdAndUsedAtIsNull(UUID userId);
}
