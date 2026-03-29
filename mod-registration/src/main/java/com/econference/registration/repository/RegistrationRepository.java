package com.econference.registration.repository;

import com.econference.registration.domain.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    List<Registration> findByEventId(UUID eventId);

    List<Registration> findByUserId(UUID userId);
}
