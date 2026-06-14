package com.interview.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendInterviewInvitation(String toEmail, String roleName, String title, String description, String scheduledAt, int durationMinutes, String roomToken) {
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
                roleName, title, description != null ? description : "N/A", scheduledAt, durationMinutes, joinUrl
            );
            
            message.setText(body);
            mailSender.send(message);
            log.info("Successfully sent interview invitation email to {} as {}", toEmail, roleName);
        } catch (Exception e) {
            log.error("Failed to send interview invitation email to {} as {}: {}", toEmail, roleName, e.getMessage(), e);
        }
    }
}
