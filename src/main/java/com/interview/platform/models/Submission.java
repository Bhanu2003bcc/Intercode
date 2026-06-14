package com.interview.platform.models;

import com.interview.platform.enums.Language;
import com.interview.platform.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private Interview interview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(columnDefinition = "TEXT")
    private String stdin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String stdout;

    @Column(columnDefinition = "TEXT")
    private String stderr;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
