package com.interview.platform.util;

import com.interview.platform.models.Interview;
import com.interview.platform.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewEventHandler {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterviewCreated(InterviewCreatedEvent event) {
        Interview i = event.getInterview();
        log.info("Sending interview invitations for interview '{}'", i.getId());

        if (i.getInterviewerEmail() != null) {
            try {
                emailService.sendInterviewInvitation(
                    i.getInterviewerEmail(), "INTERVIEWER",
                    i.getTitle(), i.getDescription(),
                    i.getScheduledAt().toString(),
                    i.getDurationMinutes(), i.getRoomToken()
                );
            } catch (Exception e) {
                log.error("Failed to send interviewer invitation for interview '{}': {}",
                    i.getId(), e.getMessage(), e);
            }
        }

        if (i.getCandidateEmail() != null) {
            try {
                emailService.sendInterviewInvitation(
                    i.getCandidateEmail(), "CANDIDATE",
                    i.getTitle(), i.getDescription(),
                    i.getScheduledAt().toString(),
                    i.getDurationMinutes(), i.getRoomToken()
                );
            } catch (Exception e) {
                log.error("Failed to send candidate invitation for interview '{}': {}",
                    i.getId(), e.getMessage(), e);
            }
        }
    }
}
