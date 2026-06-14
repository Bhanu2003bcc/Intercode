package com.interview.platform.controller;

import com.interview.platform.models.User;
import com.interview.platform.request.ExecutionRequest;
import com.interview.platform.response.ExecutionResult;
import com.interview.platform.service.CodeExecutionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/execute")
@RequiredArgsConstructor
public class ExecutionController {

    private final CodeExecutionService executionService;

    @PostMapping
    public ResponseEntity<ExecutionResult> execute(@RequestBody ExecutionRequest request,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(executionService.execute(request, user));
    }
}
