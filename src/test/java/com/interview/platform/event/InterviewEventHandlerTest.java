package com.interview.platform.event;

import com.interview.platform.models.Interview;
import com.interview.platform.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class InterviewEventHandlerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private InterviewEventHandler handler;

    private Interview testInterview;

    @BeforeEach
    void setUp() {
        testInterview = Interview.builder()
                .id(UUID.randomUUID())
                .title("System Design Interview")
                .description("Architecture round")
                .candidateEmail("candidate@test.com")
                .interviewerEmail("interviewer@test.com")
                .scheduledAt(Instant.now().plusSeconds(3600))
                .durationMinutes(60)
                .roomToken("abc123token")
                .build();
    }

    @Test
    void onInterviewCreated_sendsEmailsToBothParticipants() {
        InterviewCreatedEvent event = new InterviewCreatedEvent(this, testInterview);

        handler.onInterviewCreated(event);

        verify(emailService).sendInterviewInvitation(
                eq("interviewer@test.com"), eq("INTERVIEWER"),
                eq("System Design Interview"), eq("Architecture round"),
                eq(testInterview.getScheduledAt().toString()),
                eq(60), eq("abc123token"));

        verify(emailService).sendInterviewInvitation(
                eq("candidate@test.com"), eq("CANDIDATE"),
                eq("System Design Interview"), eq("Architecture round"),
                eq(testInterview.getScheduledAt().toString()),
                eq(60), eq("abc123token"));

        verifyNoMoreInteractions(emailService);
    }

    @Test
    void onInterviewCreated_skipsEmailWhenCandidateEmailIsNull() {
        Interview noCandidateInterview = Interview.builder()
                .id(UUID.randomUUID())
                .title("Backend Round")
                .interviewerEmail("interviewer@test.com")
                .scheduledAt(Instant.now().plusSeconds(3600))
                .durationMinutes(45)
                .roomToken("token456")
                .build();

        handler.onInterviewCreated(new InterviewCreatedEvent(this, noCandidateInterview));

        verify(emailService, times(1)).sendInterviewInvitation(
                eq("interviewer@test.com"), eq("INTERVIEWER"),
                eq("Backend Round"), eq(null),
                eq(noCandidateInterview.getScheduledAt().toString()),
                eq(45), eq("token456"));
        verifyNoMoreInteractions(emailService);
    }

    @Test
    void onInterviewCreated_skipsEmailWhenInterviewerEmailIsNull() {
        Interview noInterviewerInterview = Interview.builder()
                .id(UUID.randomUUID())
                .title("Frontend Round")
                .candidateEmail("candidate@test.com")
                .scheduledAt(Instant.now().plusSeconds(3600))
                .durationMinutes(30)
                .roomToken("token789")
                .build();

        handler.onInterviewCreated(new InterviewCreatedEvent(this, noInterviewerInterview));

        verify(emailService, times(1)).sendInterviewInvitation(
                eq("candidate@test.com"), eq("CANDIDATE"),
                eq("Frontend Round"), eq(null),
                eq(noInterviewerInterview.getScheduledAt().toString()),
                eq(30), eq("token789"));
        verifyNoMoreInteractions(emailService);
    }
}
