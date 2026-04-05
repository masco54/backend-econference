package com.econference.event.repository;

import com.econference.event.domain.EventRequest;
import com.econference.event.domain.EventRequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface EventRequestRepository extends JpaRepository<EventRequest, UUID>, JpaSpecificationExecutor<EventRequest> {

    boolean existsByRequesterEmailIgnoreCaseAndEventNameIgnoreCaseAndStatusIn(
            String requesterEmail,
            String eventName,
            Collection<EventRequestStatus> statuses
    );

    @EntityGraph(attributePaths = "statusHistory")
    @Query("""
            select er
            from EventRequest er
            where upper(er.trackingCode) = upper(:trackingCode)
              and lower(er.requesterEmail) = lower(:requesterEmail)
            """)
    Optional<EventRequest> findTracked(@Param("trackingCode") String trackingCode,
                                       @Param("requesterEmail") String requesterEmail);

    @EntityGraph(attributePaths = "statusHistory")
    @Query("select er from EventRequest er where er.id = :id")
    Optional<EventRequest> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "statusHistory")
    Optional<EventRequest> findByActivationToken(UUID activationToken);

    default Slice<EventRequest> findLatest(Pageable pageable) {
        return findAll(pageable);
    }
}