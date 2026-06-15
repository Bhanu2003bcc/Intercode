package com.interview.platform.controller;

import com.interview.platform.dto.InterviewDTO;
import com.interview.platform.service.InterviewService;
import com.interview.platform.enums.InterviewStatus;
import com.interview.platform.models.User;
import com.interview.platform.request.CreateInterviewRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERVIEWER')")
    public ResponseEntity<InterviewDTO> create(@Valid @RequestBody CreateInterviewRequest request,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(interviewService.create(request, user));
    }

    @GetMapping
    public ResponseEntity<List<InterviewDTO>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(interviewService.findAll(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.findById(id));
    }

    @GetMapping("/room/{roomToken}")
    public ResponseEntity<InterviewDTO> getByRoom(@PathVariable String roomToken) {
        return ResponseEntity.ok(interviewService.findByRoomToken(roomToken));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERVIEWER', 'CANDIDATE')")
    public ResponseEntity<InterviewDTO> updateStatus(@PathVariable UUID id,
                                                     @RequestParam InterviewStatus status,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(interviewService.updateStatus(id, status, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        interviewService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
