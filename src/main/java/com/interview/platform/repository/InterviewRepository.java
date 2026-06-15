package com.interview.platform.repository;

import com.interview.platform.enums.InterviewStatus;
import com.interview.platform.models.Interview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    Optional<Interview> findByRoomToken(String roomToken);

    Page<Interview> findByCandidateId(UUID candidateId, Pageable pageable);

    Page<Interview> findByInterviewerId(UUID interviewerId, Pageable pageable);

    @Query("""
        SELECT i FROM Interview i
        LEFT JOIN FETCH i.candidate
        LEFT JOIN FETCH i.interviewer
        ORDER BY i.scheduledAt DESC
    """)
    List<Interview> findAllWithUsers();

    @Query("""
        SELECT i FROM Interview i
        LEFT JOIN FETCH i.candidate
        LEFT JOIN FETCH i.interviewer
        WHERE i.candidate.id = :userId 
           OR i.interviewer.id = :userId 
           OR i.candidateEmail = :email 
           OR i.interviewerEmail = :email
        ORDER BY i.scheduledAt DESC
    """)
    List<Interview> findAllByParticipant(UUID userId, String email);

    @Query("""
        SELECT i FROM Interview i
        LEFT JOIN FETCH i.candidate
        LEFT JOIN FETCH i.interviewer
        WHERE i.interviewer.id = :userId 
           OR i.interviewerEmail = :email
        ORDER BY i.scheduledAt DESC
    """)
    List<Interview> findAllByInterviewer(UUID userId, String email);

    @Query("""
        SELECT i FROM Interview i
        LEFT JOIN FETCH i.candidate
        LEFT JOIN FETCH i.interviewer
        WHERE i.candidate.id = :userId 
           OR i.candidateEmail = :email
        ORDER BY i.scheduledAt DESC
    """)
    List<Interview> findAllByCandidate(UUID userId, String email);

    long countByStatus(InterviewStatus status);
}
