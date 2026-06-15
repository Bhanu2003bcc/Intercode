package com.interview.platform.request;

import java.time.Instant;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;

public record UpdateInterviewRequest(
    @NotBlank
    String title,
    String description,
    @Future 
    Instant scheduledAt,
    Integer durationMinutes
) {}