package com.interview.platform.service;

import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.platform.dto.UserDTO;
import com.interview.platform.enums.Provider;
import com.interview.platform.enums.Role;
import com.interview.platform.exception.EmailAlreadyExistsException;
import com.interview.platform.models.User;
import com.interview.platform.repository.UserRepository;
import com.interview.platform.request.LoginRequest;
import com.interview.platform.request.RegisterRequest;
import com.interview.platform.response.AuthResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // @Transactional
    // public AuthResponse register(RegisterRequest request) {
    //     if (userRepository.existsByEmail(request.email())) {
    //         throw new IllegalArgumentException("Email already registered");
    //     }

    //     User user = User.builder()
    //         .email(request.email())
    //         .passwordHash(passwordEncoder.encode(request.password()))
    //         .fullName(request.fullName())
    //         .role(request.role() != null ? request.role() : Role.CANDIDATE)
    //         .provider(Provider.LOCAL)
    //         .isActive(true)
    //         .build();

    //     userRepository.save(user);
    //     String token = jwtService.generateToken(user);
    //     return new AuthResponse(token, UserDTO.from(user));
    // }

   @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .role(Objects.requireNonNullElse(request.role(), Role.CANDIDATE))
            .provider(Provider.LOCAL)
            .isActive(true)
            .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.email());
        }

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
    // public AuthResponse login(LoginRequest request) {
    // // Single DB call — load user first, then verify credentials
    // User user = userRepository.findByEmail(request.email())
    //     .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

    // // Authenticate against the already-loaded user (no second DB hit)
    // Authentication auth = authenticationManager.authenticate(
    //     new UsernamePasswordAuthenticationToken(request.email(), request.password())
    // );

    // // Type-safe principal extraction instead of a redundant DB call
    // User authenticatedUser = (User) auth.getPrincipal();

    // String token = jwtService.generateToken(authenticatedUser);
    // return new AuthResponse(token, UserDTO.from(authenticatedUser));
    // }
}
