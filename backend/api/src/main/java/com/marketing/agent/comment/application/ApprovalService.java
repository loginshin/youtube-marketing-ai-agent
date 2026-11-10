package com.marketing.agent.comment.application;

import com.marketing.agent.db.entity.CommentJob;
import com.marketing.agent.db.entity.CommentJobEvent;
import com.marketing.agent.db.entity.JobStatus;
import com.marketing.agent.db.repository.CommentJobRepository;
import com.marketing.agent.db.repository.CommentJobEventRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {
    private final CommentJobRepository jobs;
    private final CommentJobEventRepository events;

    public ApprovalService(CommentJobRepository jobs, CommentJobEventRepository events) {
        this.jobs = jobs;
        this.events = events;
    }

    @Transactional
    public void approve(String videoId, String profile, String body) {
        if (!videoId.matches("[A-Za-z0-9_-]{11}") || profile == null || profile.isBlank()
                || profile.length() > 128 || body == null) {
            throw new IllegalArgumentException("영상, 프로필, 최종 본문을 확인해주세요.");
        }
        String text = body.strip();
        int length = text.codePointCount(0, text.length());
        if (length < 40 || length > 120 || text.contains("#")
                || java.util.regex.Pattern.compile("(?i)https?://|www\\.").matcher(text).find()) {
            throw new IllegalArgumentException("본문은 링크와 해시태그 없이 40~120자로 작성해주세요.");
        }
        Instant now = Instant.now();
        CommentJob job = jobs.save(new CommentJob(videoId, profile.strip(), text, now));
        events.save(new CommentJobEvent(job, JobStatus.APPROVED, now, null, null));
    }
}
