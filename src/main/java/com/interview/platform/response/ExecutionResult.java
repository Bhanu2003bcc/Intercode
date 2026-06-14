package com.interview.platform.response;

import java.util.UUID;

public record ExecutionResult(
    String stdout,
    String stderr,
    int exitCode,
    boolean timedOut,
    UUID submissionId
) {}
