package com.marketing.agent.analysis.work;

import jakarta.persistence.*;

@Entity
@Table(name = "video_analysis")
class VideoAnalysis {
    enum Stage { SUMMARY, DRAFT, READY, INSUFFICIENT, FAILED, CANCELLED }

    @Id String videoId;
    @Version Long version;
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    Stage stage;
    String concern = "";
    String evidence = "";
    String draft = "";
    String attempt;
    java.time.Instant startedAt;
    String failureReason;
    String analysisType;

    protected VideoAnalysis() {}
    VideoAnalysis(String id) {
        videoId = id;
        stage = Stage.SUMMARY;
        attempt = java.util.UUID.randomUUID().toString();
    }

    boolean running() { return stage == Stage.SUMMARY || stage == Stage.DRAFT; }
    Snapshot snapshot() { return new Snapshot(stage, concern, evidence, draft, attempt, startedAt, failureReason, analysisType); }
    record Snapshot(Stage stage, String concern, String evidence, String draft, String attempt, java.time.Instant startedAt, String failureReason, String analysisType) {}
}
