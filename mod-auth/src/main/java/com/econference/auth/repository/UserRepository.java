package com.econference.auth.repository;

import com.econference.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * @deprecated Use {@link com.econference.auth.adapter.out.persistence.UserJpaRepository}
 * and {@link com.econference.auth.application.port.out.UserRepositoryPort} instead.
 */
@Deprecated
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
