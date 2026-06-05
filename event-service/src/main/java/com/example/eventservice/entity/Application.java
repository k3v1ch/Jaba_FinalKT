package com.example.eventservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications",
       uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_email"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(columnDefinition = "TEXT")
    private String reviewComment;

    @CreationTimestamp
    private LocalDateTime appliedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ApplicationStatus {
        PENDING, APPROVED, REJECTED
    }
}
