package com.interview.platform.dto;

import com.interview.platform.enums.InterviewStatus;
import com.interview.platform.models.Interview;

import java.time.Instant;
import java.util.UUID;

public record InterviewDTO(
    UUID id,
    String title,
    String description,
    Instant scheduledAt,
    int durationMinutes,
    InterviewStatus status,
    UserDTO candidate,
    String candidateEmail,
    UserDTO interviewer,
    String interviewerEmail,
    String roomToken,
    String notes,
    Instant createdAt
) {
    public static InterviewDTO from(Interview i) {
        return new InterviewDTO(
            i.getId(),
            i.getTitle(),
            i.getDescription(),
            i.getScheduledAt(),
            i.getDurationMinutes(),
            i.getStatus(),
            i.getCandidate() != null ? UserDTO.from(i.getCandidate()) : null,
            i.getCandidateEmail(),
            i.getInterviewer() != null ? UserDTO.from(i.getInterviewer()) : null,
            i.getInterviewerEmail(),
            i.getRoomToken(),
            i.getNotes(),
            i.getCreatedAt()
        );
    }
}
