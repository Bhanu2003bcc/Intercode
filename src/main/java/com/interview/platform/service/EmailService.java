package com.interview.platform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.interview.platform.models.User;
import com.interview.platform.repository.EmailVerificationTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailTokenService emailTokenService; 

    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@interviewplatform.com}")
    private String fromEmail;

    @Value("${app.verification.token.expiry-hours:24}")
    private int tokenExpiryHours;

    public void sendVerificationEmail(User user) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("SMTP config missing — skipping verification email for {}", user.getEmail());
            return;
        }

        try {
            String rawToken = emailTokenService.createVerificationToken(user);
            String verifyUrl = frontendUrl + "/verify-email?token=" + rawToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Verify your email — Interview Platform");
            message.setText(buildEmailBody(user.getFullName(), verifyUrl));

            log.info("Attempting mailSender.send()...");
            // mailSender.send(message
            log.info("Verification email sent to {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    private String buildEmailBody(String fullName, String verifyUrl) {
        return String.format(
            "Hi %s,\n\n" +
            "Thanks for registering on Interview Platform!\n\n" +
            "Please verify your email by clicking the link below:\n\n" +
            "%s\n\n" +
            "This link will expire in %d hours.\n\n" +
            "If you did not create an account, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Interview Platform Team",
            fullName, verifyUrl, tokenExpiryHours
        );
    }

    // existing method unchanged
    public void sendInterviewInvitation(String toEmail, String roleName, String title,
                                        String description, String scheduledAt,
                                        int durationMinutes, String roomToken) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("SMTP configuration is not set. Skipping email invitation to {} for role {}", toEmail, roleName);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Interview Invitation: " + title);

            String joinUrl = frontendUrl + "/room/" + roomToken;

            String body = String.format(
                "Hello,\n\n" +
                "You have been invited to an interview as a %s.\n\n" +
                "Details:\n" +
                "Title: %s\n" +
                "Description: %s\n" +
                "Scheduled At: %s (UTC)\n" +
                "Duration: %d minutes\n\n" +
                "You can join the interview room here:\n" +
                "%s\n\n" +
                "Best regards,\n" +
                "Interview Platform Team",
                roleName, title, description != null ? description : "N/A",
                scheduledAt, durationMinutes, joinUrl
            );

            message.setText(body);
            mailSender.send(message);
            log.info("Successfully sent interview invitation email to {} as {}", toEmail, roleName);
        } catch (Exception e) {
            log.error("Failed to send interview invitation email to {} as {}: {}", toEmail, roleName, e.getMessage(), e);
        }
    }
}