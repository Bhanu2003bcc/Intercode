package com.interview.platform.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.interview.platform.models.EmailVerificationToken;

import jakarta.transaction.Transactional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByToken(String token);

    @Modifying
    @Transactional
    void deleteByUserId(UUID userId);
}
