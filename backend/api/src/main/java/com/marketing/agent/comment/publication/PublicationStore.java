package com.marketing.agent.comment.publication;

import com.marketing.agent.db.entity.*;
import com.marketing.agent.db.repository.*;
import com.marketing.agent.video.infrastructure.YouTubeClient.PublishedSnippet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;

@Service
public class PublicationStore {
    private final CommentJobRepository jobs;
    private final CommentJobEventRepository events;
    private final PostedCommentRepository posted;

    public PublicationStore(CommentJobRepository jobs, CommentJobEventRepository events, PostedCommentRepository posted) {
        this.jobs = jobs;
        this.events = events;
        this.posted = posted;
    }

    public CommentJob job(String video) {
        return jobs.findByVideoId(video).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // Commit before the external click. A crash or timeout must never enable another submission.
    @Transactional
    public void reserve(String video) {
        jobs.lockApprovalGuard();
        var job = job(video);
        if (job.getStatus() != JobStatus.APPROVED || !"유튜브 1".equals(job.getProfileName()))
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        var zone = ZoneId.of("Asia/Seoul");
        var today = LocalDate.now(zone);
        if (events.countByStatusAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(JobStatus.CONFIRMATION_PENDING,
                today.atStartOfDay(zone).toInstant(), today.plusDays(1).atStartOfDay(zone).toInstant()) >= 10)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        transition(job, JobStatus.CONFIRMATION_PENDING);
    }

    @Transactional
    public void remember(String video, String id) {
        jobs.lockApprovalGuard();
        var job = job(video);
        if (job.getStatus() == JobStatus.CONFIRMATION_PENDING) job.rememberComment(id);
    }

    @Transactional
    public PostedComment confirm(String video, String title, String id, PublishedSnippet snippet) {
        jobs.lockApprovalGuard();
        var job = job(video);
        var existing = posted.findByVideoId(video);
        if (existing.isPresent()) {
            if (existing.get().commentId.equals(id)) return existing.get();
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        if ((job.getStatus() != JobStatus.APPROVED && job.getStatus() != JobStatus.CONFIRMATION_PENDING)
                || (job.getPendingCommentId() != null && !job.getPendingCommentId().equals(id))
                || !video.equals(snippet.videoId()) || snippet.parentId() != null
                || !job.getBody().equals(snippet.textDisplay()) || snippet.authorChannelId() == null
                || snippet.authorChannelId().value() == null || snippet.authorDisplayName() == null
                || snippet.publishedAt() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        var row = posted.save(new PostedComment(job, title, id, snippet));
        transition(job, JobStatus.COMPLETED);
        return row;
    }

    @Transactional
    public void recordFailure(String video, String reason, String screenshot) {
        events.save(new CommentJobEvent(job(video), JobStatus.FAILED, Instant.now(), reason, screenshot));
    }

    private void transition(CommentJob job, JobStatus status) {
        job.setPublicationStatus(status);
        events.save(new CommentJobEvent(job, status, Instant.now(), null, null));
    }
}
