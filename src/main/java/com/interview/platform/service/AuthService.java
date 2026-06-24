package com.interview.platform.service;

import java.time.Instant;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.platform.dto.UserDTO;
import com.interview.platform.enums.Provider;
import com.interview.platform.enums.Role;
import com.interview.platform.event.UserRegisteredEvent;
import com.interview.platform.exception.EmailAlreadyExistsException;
import com.interview.platform.exception.InvalidTokenException;
import com.interview.platform.models.EmailVerificationToken;
import com.interview.platform.models.User;
import com.interview.platform.repository.EmailVerificationTokenRepository;
import com.interview.platform.repository.UserRepository;
import com.interview.platform.request.LoginRequest;
import com.interview.platform.request.RegisterRequest;
import com.interview.platform.response.AuthResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailVerificationTokenRepository tokenRepository;


   @Transactional
    // 1. Back to existsByEmail — simpler and more predictable than catching DB exceptions
    public AuthResponse register(RegisterRequest request) {
    
    // Fast indexed lookup — negligible cost
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

    // Offload slow BCrypt to virtual thread or async executor
        String hashedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
        .email(request.email())
        .passwordHash(hashedPassword)
        .fullName(request.fullName())
        .role(Objects.requireNonNullElse(request.role(), Role.CANDIDATE))
        .provider(Provider.LOCAL)
        .isActive(false) 
        .build();

    userRepository.save(user);

    // Async — don't block HTTP thread for email sending
    eventPublisher.publishEvent(new UserRegisteredEvent(user));

    String token = jwtService.generateToken(user);
    return new AuthResponse(token, UserDTO.from(user));
}
    
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, UserDTO.from(user));
    }

    @Transactional
    public void verifyEmail(String token) {

        log.info("Verifying token: {}", token);
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification token"));

        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("Token already used");
        }

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Token has expired");
        }

    // Activate user
        User user = verificationToken.getUser();
        user.setActive(true);
        userRepository.save(user);

    // Mark token as used
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        log.info("Email verified successfully for {}", user.getEmail());
    }
    
}
