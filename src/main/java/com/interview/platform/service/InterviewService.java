package com.interview.platform.service;

import com.interview.platform.dto.InterviewDTO;
import com.interview.platform.repository.InterviewRepository;
import com.interview.platform.enums.InterviewStatus;
import com.interview.platform.request.CreateInterviewRequest;
import com.interview.platform.enums.Role;
import com.interview.platform.models.Interview;
import com.interview.platform.models.User;
import com.interview.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public InterviewDTO create(CreateInterviewRequest req, User createdBy) {
        User candidate = req.candidateEmail() != null
            ? userRepository.findByEmail(req.candidateEmail().trim()).orElse(null) : null;
        User interviewer = req.interviewerEmail() != null
            ? userRepository.findByEmail(req.interviewerEmail().trim()).orElse(null) : null;

        Interview interview = Interview.builder()
            .title(req.title())
            .description(req.description())
            .scheduledAt(req.scheduledAt())
            .durationMinutes(req.durationMinutes() > 0 ? req.durationMinutes() : 60)
            .candidate(candidate)
            .candidateEmail(req.candidateEmail() != null ? req.candidateEmail().trim() : null)
            .interviewer(interviewer)
            .interviewerEmail(req.interviewerEmail() != null ? req.interviewerEmail().trim() : null)
            .createdBy(createdBy)
            .roomToken(generateRoomToken())
            .status(InterviewStatus.SCHEDULED)
            .build();

        Interview saved = interviewRepository.save(interview);

        if (saved.getInterviewerEmail() != null) {
            emailService.sendInterviewInvitation(
                saved.getInterviewerEmail(),
                "INTERVIEWER",
                saved.getTitle(),
                saved.getDescription(),
                saved.getScheduledAt().toString(),
                saved.getDurationMinutes(),
                saved.getRoomToken()
            );
        }
        if (saved.getCandidateEmail() != null) {
            emailService.sendInterviewInvitation(
                saved.getCandidateEmail(),
                "CANDIDATE",
                saved.getTitle(),
                saved.getDescription(),
                saved.getScheduledAt().toString(),
                saved.getDurationMinutes(),
                saved.getRoomToken()
            );
        }

        return InterviewDTO.from(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewDTO> findAll(User requestingUser) {
        return switch (requestingUser.getRole()) {
            case ADMIN -> interviewRepository.findAllWithUsers().stream()
                .map(InterviewDTO::from).toList();
            default -> interviewRepository.findAllByParticipant(requestingUser.getId(), requestingUser.getEmail()).stream()
                .map(InterviewDTO::from).toList();
        };
    }

    @Transactional(readOnly = true)
    public InterviewDTO findById(UUID id) {
        return InterviewDTO.from(interviewRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + id)));
    }

    @Transactional(readOnly = true)
    public InterviewDTO findByRoomToken(String roomToken) {
        return InterviewDTO.from(interviewRepository.findByRoomToken(roomToken)
            .orElseThrow(() -> new IllegalArgumentException("Interview room not found")));
    }

    @Transactional
    public InterviewDTO updateStatus(UUID id, InterviewStatus status, User requestingUser) {
        Interview interview = interviewRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Interview not found"));

        // Only ADMIN, the assigned interviewer, or the assigned candidate may update status
        boolean isAdmin      = requestingUser.getRole() == Role.ADMIN;
        boolean isInterviewer = (interview.getInterviewer() != null
            && interview.getInterviewer().getId().equals(requestingUser.getId()))
            || (interview.getInterviewerEmail() != null && interview.getInterviewerEmail().equalsIgnoreCase(requestingUser.getEmail()));
        boolean isCandidate   = (interview.getCandidate() != null
            && interview.getCandidate().getId().equals(requestingUser.getId()))
            || (interview.getCandidateEmail() != null && interview.getCandidateEmail().equalsIgnoreCase(requestingUser.getEmail()));

        if (!isAdmin && !isInterviewer && !isCandidate) {
            throw new AccessDeniedException("You are not a participant of this interview");
        }

        interview.setStatus(status);
        return InterviewDTO.from(interviewRepository.save(interview));
    }

    @Transactional
    public void delete(UUID id) {
        interviewRepository.deleteById(id);
    }

    private String generateRoomToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
