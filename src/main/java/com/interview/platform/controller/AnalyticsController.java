package com.interview.platform.controller;

import com.interview.platform.repository.InterviewRepository;
import com.interview.platform.enums.InterviewStatus;
import com.interview.platform.repository.SubmissionRepository;
import com.interview.platform.enums.Role;
import com.interview.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INTERVIEWER')")
public class AnalyticsController {

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of(
            "totalInterviews", interviewRepository.count(),
            "scheduledInterviews", interviewRepository.countByStatus(InterviewStatus.SCHEDULED),
            "inProgressInterviews", interviewRepository.countByStatus(InterviewStatus.IN_PROGRESS),
            "completedInterviews", interviewRepository.countByStatus(InterviewStatus.COMPLETED),
            "cancelledInterviews", interviewRepository.countByStatus(InterviewStatus.CANCELLED),
            "totalCandidates", userRepository.findByRole(Role.CANDIDATE).size(),
            "totalInterviewers", userRepository.findByRole(Role.INTERVIEWER).size(),
            "totalSubmissions", submissionRepository.count()
        );
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERVIEWER')")
    public Map<String, Object> users() {
        return Map.of(
            "candidates", userRepository.findActiveByRole(Role.CANDIDATE),
            "interviewers", userRepository.findActiveByRole(Role.INTERVIEWER)
        );
    }
}
