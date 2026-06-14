package com.interview.platform.models;

import com.interview.platform.enums.InterviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private int durationMinutes = 60;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private User candidate;

    @Column(name = "candidate_email")
    private String candidateEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id")
    private User interviewer;

    @Column(name = "interviewer_email")
    private String interviewerEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "room_token", nullable = false, unique = true)
    private String roomToken;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }
}
