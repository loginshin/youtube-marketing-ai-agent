package com.marketing.agent.db.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comment_job_event")
public class CommentJobEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private CommentJob job;

    @Column(length = 1000)
    private String failureReason;

    @Column(length = 512)
    private String screenshotPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status;

    @Column(nullable = false)
    private Instant occurredAt;

    protected CommentJobEvent() {}

    public CommentJobEvent(CommentJob job, JobStatus status, Instant occurredAt, String failureReason, String screenshotPath) {
        this.job = job;
        this.status = status;
        this.occurredAt = occurredAt;
        this.failureReason = failureReason;
        this.screenshotPath = screenshotPath;
    }

    public JobStatus getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }

    public Long getId() { return id; }
}
