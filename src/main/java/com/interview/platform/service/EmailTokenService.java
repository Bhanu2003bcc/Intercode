package com.interview.platform.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.platform.models.EmailVerificationToken;
import com.interview.platform.models.User;
import com.interview.platform.repository.EmailVerificationTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTokenService {

    private final EmailVerificationTokenRepository tokenRepository;

    @Value("${app.verification.token.expiry-hours:24}")
    private int tokenExpiryHours;

    @Transactional
    public String createVerificationToken(User user) {
        tokenRepository.deleteByUserId(user.getId());

        String rawToken = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
            .user(user)
            .token(rawToken)
            .expiresAt(Instant.now().plusSeconds(tokenExpiryHours * 3600L))
            .used(false)
            .build();

        tokenRepository.save(verificationToken);
        log.info("Token saved for user: {}", user.getEmail());

        return rawToken;
    }
}
