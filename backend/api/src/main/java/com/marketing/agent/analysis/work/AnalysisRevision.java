package com.marketing.agent.analysis.work;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "analysis_revision")
class AnalysisRevision {
    @Id String attempt;
    String videoId;
    String outcome;
    String evidence;
    String draft;
    Instant recordedAt;
    String failureReason;
    @jakarta.persistence.Column(columnDefinition = "text")
    String screenshot;

    protected AnalysisRevision() {}

    AnalysisRevision(VideoAnalysis job) {
        failureReason = job.failureReason;
        attempt = job.attempt;
        videoId = job.videoId;
        outcome = job.stage.name();
        evidence = job.evidence;
        draft = job.stage == VideoAnalysis.Stage.READY ? job.draft : "";
        recordedAt = Instant.now();
    }
}
