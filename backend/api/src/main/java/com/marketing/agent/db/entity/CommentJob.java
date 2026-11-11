package com.marketing.agent.db.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comment_job")
public class CommentJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String videoId;

    @Column(nullable = false, length = 128)
    private String profileName;

    @Column(nullable = false, length = 240)
    private String body;

    @Version
    private long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected CommentJob() {}

    public CommentJob(String videoId, String profileName, String body, Instant createdAt) {
        this.videoId = videoId;
        this.profileName = profileName;
        this.body = body;
        this.createdAt = createdAt;
        this.status = JobStatus.APPROVED;
    }

    public String getVideoId() { return videoId; }
    public String getProfileName() { return profileName; }
    public String getBody() { return body; }
    public JobStatus getStatus() { return status; }
    private String pendingCommentId;
    public String getPendingCommentId() { return pendingCommentId; }
    public void rememberComment(String id) { pendingCommentId = id; }
    public void setPublicationStatus(JobStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }

    public Long getId() { return id; }
}
