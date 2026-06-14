package com.interview.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.platform.models.Submission;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findByInterviewIdOrderByCreatedAtDesc(UUID interviewId);
    List<Submission> findBySubmittedByIdOrderByCreatedAtDesc(UUID userId);
}
