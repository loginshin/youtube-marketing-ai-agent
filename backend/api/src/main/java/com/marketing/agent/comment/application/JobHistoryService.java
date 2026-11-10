package com.marketing.agent.comment.application;

import com.marketing.agent.db.entity.JobStatus;
import com.marketing.agent.db.repository.CommentJobRepository;
import com.marketing.agent.db.repository.CommentJobEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobHistoryService {
    private final CommentJobRepository jobs;
    private final CommentJobEventRepository events;

    public JobHistoryService(CommentJobRepository jobs, CommentJobEventRepository events) {
        this.jobs = jobs;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Optional<JobHistory> find(String videoId) {
        return jobs.findByVideoId(videoId).map(job -> {
            List<Event> history = events.findByJob_IdOrderByOccurredAtAscIdAsc(job.getId()).stream()
                    .map(event -> new Event(event.getStatus(), event.getOccurredAt())).toList();
            return new JobHistory(job.getVideoId(), job.getProfileName(), job.getBody(),
                    job.getStatus(), job.getCreatedAt(), history);
        });
    }

    public record Event(JobStatus status, Instant occurredAt) {}
    public record JobHistory(String videoId, String profile, String body, JobStatus status,
                             Instant createdAt, List<Event> events) {}
}
