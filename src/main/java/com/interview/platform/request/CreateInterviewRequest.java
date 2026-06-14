package com.interview.platform.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record CreateInterviewRequest(
    @NotBlank String title,
    String description,
    @NotNull @Future Instant scheduledAt,
    @Positive int durationMinutes,
    @NotBlank String candidateEmail,
    @NotBlank String interviewerEmail
) {}
