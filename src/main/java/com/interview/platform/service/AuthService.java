package com.interview.platform.service;

import com.interview.platform.request.LoginRequest;
import com.interview.platform.request.RegisterRequest;
import com.interview.platform.dto.UserDTO;
import com.interview.platform.enums.Provider;
import com.interview.platform.enums.Role;
import com.interview.platform.models.User;
import com.interview.platform.repository.UserRepository;
import com.interview.platform.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .role(request.role() != null ? request.role() : Role.CANDIDATE)
            .provider(Provider.LOCAL)
            .isActive(true)
            .build();

        userRepository.save(user);
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
}
