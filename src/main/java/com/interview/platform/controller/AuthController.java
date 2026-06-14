package com.interview.platform.controller;

import com.interview.platform.response.AuthResponse;
import com.interview.platform.dto.UserDTO;
import com.interview.platform.models.User;
import com.interview.platform.service.AuthService;
import com.interview.platform.request.LoginRequest;
import com.interview.platform.request.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserDTO.from(user));
    }
}
