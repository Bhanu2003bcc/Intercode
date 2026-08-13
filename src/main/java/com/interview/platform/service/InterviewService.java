package com.interview.platform.service;

import com.interview.platform.dto.InterviewDTO;
import com.interview.platform.enums.InterviewStatus;
import com.interview.platform.enums.Role;
import com.interview.platform.exception.InterviewNotFoundException;
import com.interview.platform.exception.InterviewConflictException;
import com.interview.platform.models.Interview;
import com.interview.platform.models.User;
import com.interview.platform.repository.InterviewRepository;
import com.interview.platform.repository.UserRepository;
import com.interview.platform.request.CreateInterviewRequest;
import com.interview.platform.request.UpdateInterviewRequest;
import com.interview.platform.event.InterviewCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for Interview lifecycle management.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Email notifications are decoupled via {@link ApplicationEventPublisher} and a
 *       {@link org.springframework.transaction.event.TransactionalEventListener} in
 *       {@code InterviewEventHandler}, so emails are only sent after the DB transaction commits.
 *   <li>Room tokens are generated with {@link SecureRandom} for cryptographic strength.
 *   <li>Optimistic locking is enforced via {@code @Version} on the {@link Interview} entity.
 *   <li>All "not found" cases throw {@link InterviewNotFoundException} (mapped to HTTP 404
 *       by the global exception handler).
 *   <li>Authorization follows the principle of least privilege: only ADMINs or direct
 *       participants may mutate an interview; only ADMINs may delete.
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final int ROOM_TOKEN_BYTES = 16; 
    private static final int MIN_DURATION_MINUTES = 15;
    private static final int DEFAULT_DURATION_MINUTES = 60;
    private static final int MAX_DURATION_MINUTES = 480; // 8 hours

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom; // injected as a bean (singleton, thread-safe)


    // Create

    @Transactional
    public InterviewDTO create(CreateInterviewRequest req, User createdBy) {
        log.info("Creating interview '{}' by user '{}'", req.title(), createdBy.getId());

        validateScheduledAt(req.scheduledAt());
        int duration = validateAndNormalizeDuration(req.durationMinutes());

        User candidate   = resolveUserByEmail(req.candidateEmail()).orElse(null);
        User interviewer = resolveUserByEmail(req.interviewerEmail()).orElse(null);

        Interview interview = Interview.builder()
                .title(req.title().strip())
                .description(req.description() != null ? req.description().strip() : null)
                .scheduledAt(req.scheduledAt())
                .durationMinutes(duration)
                .candidate(candidate)
                .candidateEmail(normalizeEmail(req.candidateEmail()))
                .interviewer(interviewer)
                .interviewerEmail(normalizeEmail(req.interviewerEmail()))
                .createdBy(createdBy)
                .roomToken(generateRoomToken())
                .status(InterviewStatus.SCHEDULED)
                .build();

        Interview saved = interviewRepository.save(interview);
        log.info("Interview '{}' created with id='{}' roomToken='{}'",
                saved.getTitle(), saved.getId(), saved.getRoomToken());

        // Publish event — email sending happens AFTER commit via @TransactionalEventListener
        eventPublisher.publishEvent(new InterviewCreatedEvent(this, saved));

        return InterviewDTO.from(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewDTO> findAll(User requestingUser) {
        log.debug("findAll called by user '{}' with role '{}'",
                requestingUser.getId(), requestingUser.getRole());

        return switch (requestingUser.getRole()) {
            case ADMIN -> interviewRepository.findAllWithUsers()
                    .stream()
                    .map(InterviewDTO::from)
                    .toList();
            case INTERVIEWER -> interviewRepository
                    .findAllByInterviewer(requestingUser.getId(), requestingUser.getEmail())
                    .stream()
                    .map(InterviewDTO::from)
                    .toList();
            case CANDIDATE -> interviewRepository
                    .findAllByCandidate(requestingUser.getId(), requestingUser.getEmail())
                    .stream()
                    .map(InterviewDTO::from)
                    .toList();
            default -> {
                log.warn("Unrecognized role '{}' for user '{}'",
                        requestingUser.getRole(), requestingUser.getId());
                throw new AccessDeniedException("Your role is not permitted to list interviews");
            }
        };
    }

    @Transactional(readOnly = true)
    public InterviewDTO findById(UUID id) {
        log.debug("findById '{}'", id);
        return InterviewDTO.from(requireInterview(id));
    }

    @Transactional(readOnly = true)
    public InterviewDTO findByRoomToken(String roomToken) {
        log.debug("findByRoomToken '{}'", roomToken);
        return InterviewDTO.from(
                interviewRepository.findByRoomToken(roomToken)
                        .orElseThrow(() -> new InterviewNotFoundException(
                                "Interview room not found for token: " + roomToken))
        );
    }

    // Update

    @Transactional
    public InterviewDTO update(UUID id, UpdateInterviewRequest req, User requestingUser) {
        log.info("Updating interview '{}' by user '{}'", id, requestingUser.getId());

        Interview interview = requireInterview(id);
        requireAdminOrCreator(interview, requestingUser, "update");

        if (StringUtils.hasText(req.title())) {
            interview.setTitle(req.title().strip());
        }
        if (req.description() != null) {
            interview.setDescription(req.description().strip());
        }
        if (req.scheduledAt() != null) {
            validateScheduledAt(req.scheduledAt());
            interview.setScheduledAt(req.scheduledAt());
        }
        if (req.durationMinutes() != null) {
            interview.setDurationMinutes(validateAndNormalizeDuration(req.durationMinutes()));
        }

        Interview saved = interviewRepository.save(interview);
        log.info("Interview '{}' updated successfully", id);
        return InterviewDTO.from(saved);
    }

    
       
    @Transactional
    public InterviewDTO updateStatus(UUID id, InterviewStatus newStatus, User requestingUser) {
        log.info("Status update requested for interview '{}' → '{}' by user '{}'",
                id, newStatus, requestingUser.getId());

        Interview interview = requireInterview(id);
        requireParticipantOrAdmin(interview, requestingUser, "update status of");
        validateStatusTransition(interview.getStatus(), newStatus);

        interview.setStatus(newStatus);
        Interview saved = interviewRepository.save(interview);
        log.info("Interview '{}' status changed: '{}' → '{}'",
                id, interview.getStatus(), newStatus);
        return InterviewDTO.from(saved);
    }


    // Delete

    @Transactional
    public void delete(UUID id, User requestingUser) {
        log.warn("Delete requested for interview '{}' by user '{}'", id, requestingUser.getId());

        Interview interview = requireInterview(id);
        requireAdmin(requestingUser, "delete");

        interviewRepository.delete(interview);
        log.warn("Interview '{}' deleted by admin '{}'", id, requestingUser.getId());
    }

    
    // Private helpers — authorization

    private void requireAdmin(User user, String action) {
        if (user.getRole() != Role.ADMIN) {
            log.warn("User '{}' with role '{}' attempted to {} an interview without ADMIN role",
                    user.getId(), user.getRole(), action);
            throw new AccessDeniedException("Only ADMINs may " + action + " an interview");
        }
    }

    private void requireAdminOrCreator(Interview interview, User user, String action) {
        boolean isAdmin   = user.getRole() == Role.ADMIN;
        boolean isCreator = interview.getCreatedBy() != null
                && interview.getCreatedBy().getId().equals(user.getId());
        if (!isAdmin && !isCreator) {
            log.warn("User '{}' attempted to {} interview '{}' without permission",
                    user.getId(), action, interview.getId());
            throw new AccessDeniedException("Only ADMINs or the creator may " + action + " this interview");
        }
    }

    private void requireParticipantOrAdmin(Interview interview, User user, String action) {
        if (isAdmin(user) || isInterviewer(interview, user) || isCandidate(interview, user)) {
            return;
        }
        log.warn("User '{}' attempted to {} interview '{}' but is not a participant",
                user.getId(), action, interview.getId());
        throw new AccessDeniedException("You are not a participant of this interview");
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private boolean isInterviewer(Interview interview, User user) {
        return (interview.getInterviewer() != null
                && interview.getInterviewer().getId().equals(user.getId()))
                || (interview.getInterviewerEmail() != null
                && interview.getInterviewerEmail().equalsIgnoreCase(user.getEmail()));
    }

    private boolean isCandidate(Interview interview, User user) {
        return (interview.getCandidate() != null
                && interview.getCandidate().getId().equals(user.getId()))
                || (interview.getCandidateEmail() != null
                && interview.getCandidateEmail().equalsIgnoreCase(user.getEmail()));
    }

    // Private helpers — validation
    private void validateScheduledAt(Instant scheduledAt) {
        if (scheduledAt == null) {
            throw new IllegalArgumentException("scheduledAt must not be null");
        }
        if (!scheduledAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("scheduledAt must be a future date/time");
        }
    }

    private int validateAndNormalizeDuration(int durationMinutes) {
        if (durationMinutes <= 0) {
            log.debug("durationMinutes '{}' invalid, defaulting to {}", durationMinutes, DEFAULT_DURATION_MINUTES);
            return DEFAULT_DURATION_MINUTES;
        }
        if (durationMinutes < MIN_DURATION_MINUTES) {
            throw new IllegalArgumentException(
                    "Duration must be at least " + MIN_DURATION_MINUTES + " minutes");
        }
        if (durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException(
                    "Duration must not exceed " + MAX_DURATION_MINUTES + " minutes");
        }
        return durationMinutes;
    }

    private void validateStatusTransition(InterviewStatus current, InterviewStatus next) {
        boolean valid = switch (current) {
            case SCHEDULED    -> next == InterviewStatus.IN_PROGRESS
                                 || next == InterviewStatus.CANCELLED;
            case IN_PROGRESS  -> next == InterviewStatus.COMPLETED
                                 || next == InterviewStatus.CANCELLED;
            case COMPLETED,
                 CANCELLED    -> false; // terminal states
        };
        if (!valid) {
            throw new InterviewConflictException(
                    "Cannot transition interview from " + current + " to " + next);
        }
    }

    // Private helpers — utilities

    private Interview requireInterview(UUID id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new InterviewNotFoundException("Interview not found: " + id));
    }

    private Optional<User> resolveUserByEmail(String email) {
        if (!StringUtils.hasText(email)) return Optional.empty();
        return userRepository.findByEmail(email.strip().toLowerCase());
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.strip().toLowerCase() : null;
    }

    
    private String generateRoomToken() {
        byte[] bytes = new byte[ROOM_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
