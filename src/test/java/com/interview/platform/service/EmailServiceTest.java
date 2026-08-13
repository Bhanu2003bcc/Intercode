package com.interview.platform.service;

import com.interview.platform.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailTokenService emailTokenService;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@interviewplatform.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5173");
    }

    @Test
    void sendInterviewInvitation_constructsAndSendsEmail() {
        emailService.sendInterviewInvitation(
                "candidate@example.com",
                "CANDIDATE",
                "Frontend Round",
                "React + JS coding",
                "2026-08-15T10:00:00Z",
                45,
                "room-token-123"
        );

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage captured = messageCaptor.getValue();
        assertEquals("noreply@interviewplatform.com", captured.getFrom());
        assertArrayEquals(new String[]{"candidate@example.com"}, captured.getTo());
        assertEquals("Interview Invitation: Frontend Round", captured.getSubject());
        assertNotNull(captured.getText());
        assertTrue(captured.getText().contains("http://localhost:5173/room/room-token-123"));
        assertTrue(captured.getText().contains("Frontend Round"));
        assertTrue(captured.getText().contains("CANDIDATE"));
    }
}
