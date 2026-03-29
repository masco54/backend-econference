package com.econference.submission.repository;

import com.econference.submission.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByEventId(UUID eventId);

    List<Submission> findByAuthorId(UUID authorId);
}
