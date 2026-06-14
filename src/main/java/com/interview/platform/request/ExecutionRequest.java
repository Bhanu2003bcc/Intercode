package com.interview.platform.request;

import com.interview.platform.enums.Language;
import java.util.UUID;

public record ExecutionRequest(
    UUID interviewId,
    Language language,
    String code,
    String stdin
) {}
