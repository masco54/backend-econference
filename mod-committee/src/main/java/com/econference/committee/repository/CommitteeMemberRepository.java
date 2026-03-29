package com.econference.committee.repository;

import com.econference.committee.domain.CommitteeMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommitteeMemberRepository extends JpaRepository<CommitteeMember, UUID> {

    List<CommitteeMember> findByEventId(UUID eventId);

    List<CommitteeMember> findByUserId(UUID userId);
}
