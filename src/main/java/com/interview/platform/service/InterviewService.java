// package com.interview.platform.service;

// import com.interview.platform.dto.InterviewDTO;
// import com.interview.platform.repository.InterviewRepository;
// import com.interview.platform.enums.InterviewStatus;
// import com.interview.platform.request.CreateInterviewRequest;
// import com.interview.platform.enums.Role;
// import com.interview.platform.models.Interview;
// import com.interview.platform.models.User;
// import com.interview.platform.repository.UserRepository;
// import lombok.RequiredArgsConstructor;
// import org.springframework.security.access.AccessDeniedException;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.List;
// import java.util.UUID;

// @Service
// @RequiredArgsConstructor
// public class InterviewService {

//     private final InterviewRepository interviewRepository;
//     private final UserRepository userRepository;
//     private final EmailService emailService;

//     @Transactional
//     public InterviewDTO create(CreateInterviewRequest req, User createdBy) {
//         User candidate = req.candidateEmail() != null
//             ? userRepository.findByEmail(req.candidateEmail().trim()).orElse(null) : null;
//         User interviewer = req.interviewerEmail() != null
//             ? userRepository.findByEmail(req.interviewerEmail().trim()).orElse(null) : null;

//         Interview interview = Interview.builder()
//             .title(req.title())
//             .description(req.description())
//             .scheduledAt(req.scheduledAt())
//             .durationMinutes(req.durationMinutes() > 0 ? req.durationMinutes() : 60)
//             .candidate(candidate)
//             .candidateEmail(req.candidateEmail() != null ? req.candidateEmail().trim() : null)
//             .interviewer(interviewer)
//             .interviewerEmail(req.interviewerEmail() != null ? req.interviewerEmail().trim() : null)
//             .createdBy(createdBy)
//             .roomToken(generateRoomToken())
//             .status(InterviewStatus.SCHEDULED)
//             .build();

//         Interview saved = interviewRepository.save(interview);

//         if (saved.getInterviewerEmail() != null) {
//             emailService.sendInterviewInvitation(
//                 saved.getInterviewerEmail(),
//                 "INTERVIEWER",
//                 saved.getTitle(),
//                 saved.getDescription(),
//                 saved.getScheduledAt().toString(),
//                 saved.getDurationMinutes(),
//                 saved.getRoomToken()
//             );
//         }
//         if (saved.getCandidateEmail() != null) {
//             emailService.sendInterviewInvitation(
//                 saved.getCandidateEmail(),
//                 "CANDIDATE",
//                 saved.getTitle(),
//                 saved.getDescription(),
//                 saved.getScheduledAt().toString(),
//                 saved.getDurationMinutes(),
//                 saved.getRoomToken()
//             );
//         }

//         return InterviewDTO.from(saved);
//     }

//     @Transactional(readOnly = true)
//     public List<InterviewDTO> findAll(User requestingUser) {
//         return switch (requestingUser.getRole()) {
//             case ADMIN -> interviewRepository.findAllWithUsers().stream()
//                 .map(InterviewDTO::from).toList();
//             default -> interviewRepository.findAllByParticipant(requestingUser.getId(), requestingUser.getEmail()).stream()
//                 .map(InterviewDTO::from).toList();
//         };
//     }

//     @Transactional(readOnly = true)
//     public InterviewDTO findById(UUID id) {
//         return InterviewDTO.from(interviewRepository.findById(id)
//             .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + id)));
//     }

//     @Transactional(readOnly = true)
//     public InterviewDTO findByRoomToken(String roomToken) {
//         return InterviewDTO.from(interviewRepository.findByRoomToken(roomToken)
//             .orElseThrow(() -> new IllegalArgumentException("Interview room not found")));
//     }

//     @Transactional
//     public InterviewDTO updateStatus(UUID id, InterviewStatus status, User requestingUser) {
//         Interview interview = interviewRepository.findById(id)
//             .orElseThrow(() -> new IllegalArgumentException("Interview not found"));

//         // Only ADMIN, the assigned interviewer, or the assigned candidate may update status
//         boolean isAdmin      = requestingUser.getRole() == Role.ADMIN;
//         boolean isInterviewer = (interview.getInterviewer() != null
//             && interview.getInterviewer().getId().equals(requestingUser.getId()))
//             || (interview.getInterviewerEmail() != null && interview.getInterviewerEmail().equalsIgnoreCase(requestingUser.getEmail()));
//         boolean isCandidate   = (interview.getCandidate() != null
//             && interview.getCandidate().getId().equals(requestingUser.getId()))
//             || (interview.getCandidateEmail() != null && interview.getCandidateEmail().equalsIgnoreCase(requestingUser.getEmail()));

//         if (!isAdmin && !isInterviewer && !isCandidate) {
//             throw new AccessDeniedException("You are not a participant of this interview");
//         }

//         interview.setStatus(status);
//         return InterviewDTO.from(interviewRepository.save(interview));
//     }

//     @Transactional
//     public void delete(UUID id) {
//         interviewRepository.deleteById(id);
//     }

//     private String generateRoomToken() {
//         return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
//     }
// }

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
import com.interview.platform.util.InterviewCreatedEvent;

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

    private static final int ROOM_TOKEN_BYTES = 16; // 128-bit entropy → 32-char hex string
    private static final int MIN_DURATION_MINUTES = 15;
    private static final int DEFAULT_DURATION_MINUTES = 60;
    private static final int MAX_DURATION_MINUTES = 480; // 8 hours

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom; // injected as a bean (singleton, thread-safe)

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    /**
     * Creates a new interview, persists it, and publishes an event so that
     * invitation emails are dispatched <em>after</em> the transaction commits.
     *
     * @param req       validated creation request
     * @param createdBy the authenticated user performing this action
     * @return the persisted interview as a DTO
     * @throws IllegalArgumentException if the scheduled time is in the past or duration is invalid
     */
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

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns all interviews visible to the requesting user.
     * ADMINs see everything; all other roles see only interviews they participate in.
     */
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

    /**
     * Finds an interview by its primary key.
     *
     * @throws InterviewNotFoundException if no interview exists with the given id
     */
    @Transactional(readOnly = true)
    public InterviewDTO findById(UUID id) {
        log.debug("findById '{}'", id);
        return InterviewDTO.from(requireInterview(id));
    }

    /**
     * Finds an interview by its room token (used for joining a live session).
     *
     * @throws InterviewNotFoundException if the token does not match any interview
     */
    @Transactional(readOnly = true)
    public InterviewDTO findByRoomToken(String roomToken) {
        log.debug("findByRoomToken '{}'", roomToken);
        return InterviewDTO.from(
                interviewRepository.findByRoomToken(roomToken)
                        .orElseThrow(() -> new InterviewNotFoundException(
                                "Interview room not found for token: " + roomToken))
        );
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    /**
     * Updates mutable fields of an interview (title, description, scheduledAt, duration).
     * Only the creating ADMIN or an ADMIN role may perform a full update.
     *
     * @throws InterviewNotFoundException if the interview does not exist
     * @throws AccessDeniedException      if the caller is not authorised
     */
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

    /**
     * Transitions the status of an interview.
     * Only ADMINs, the assigned interviewer, or the assigned candidate may call this.
     *
     * @throws InterviewNotFoundException if the interview does not exist
     * @throws AccessDeniedException      if the caller is not a participant or admin
     * @throws InterviewConflictException if the status transition is illegal
     */
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

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    /**
     * Hard-deletes an interview. Only ADMINs may delete.
     *
     * @throws InterviewNotFoundException if the interview does not exist
     * @throws AccessDeniedException      if the caller is not an ADMIN
     */
    @Transactional
    public void delete(UUID id, User requestingUser) {
        log.warn("Delete requested for interview '{}' by user '{}'", id, requestingUser.getId());

        Interview interview = requireInterview(id);
        requireAdmin(requestingUser, "delete");

        interviewRepository.delete(interview);
        log.warn("Interview '{}' deleted by admin '{}'", id, requestingUser.getId());
    }

    // -------------------------------------------------------------------------
    // Private helpers — authorization
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Private helpers — validation
    // -------------------------------------------------------------------------

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

    /**
     * Validates that the requested status transition follows allowed business rules.
     *
     * <pre>
     * SCHEDULED → IN_PROGRESS → COMPLETED
     *           ↘ CANCELLED
     * </pre>
     */
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

    // -------------------------------------------------------------------------
    // Private helpers — utilities
    // -------------------------------------------------------------------------

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

    /**
     * Generates a cryptographically secure, URL-safe 32-character hex room token.
     * 128 bits of entropy makes brute-force enumeration infeasible.
     */
    private String generateRoomToken() {
        byte[] bytes = new byte[ROOM_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
