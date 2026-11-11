package com.marketing.agent.comment.application;

import com.marketing.agent.ai.CommentFormat;
import com.marketing.agent.db.entity.CommentJob;
import com.marketing.agent.db.entity.CommentJobEvent;
import com.marketing.agent.db.entity.JobStatus;
import com.marketing.agent.db.repository.CommentJobRepository;
import com.marketing.agent.db.repository.CommentJobEventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {
    private static final ZoneId APPROVAL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int DAILY_LIMIT = 10;

    private final CommentJobRepository jobs;
    private final CommentJobEventRepository events;

    public ApprovalService(CommentJobRepository jobs, CommentJobEventRepository events) {
        this.jobs = jobs;
        this.events = events;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void approve(String videoId, String profile, String body) {
        if (!videoId.matches("[A-Za-z0-9_-]{11}") || profile == null || profile.isBlank()
                || profile.length() > 128 || body == null) {
            throw new IllegalArgumentException("영상, 프로필, 최종 본문을 확인해주세요.");
        }
        String text = body.strip();
        if (!CommentFormat.isValid(text)) {
            throw new IllegalArgumentException("본문은 링크와 해시태그 없이 40~120자로 작성해주세요.");
        }
        // Hold this database lock through commit, including the event insert.
        jobs.lockApprovalGuard();
        if (jobs.existsByVideoId(videoId)) {
            throw new ApprovalRejected("이미 승인된 영상입니다.");
        }
        Instant now = Instant.now();
        LocalDate today = now.atZone(APPROVAL_ZONE).toLocalDate();
        Instant start = today.atStartOfDay(APPROVAL_ZONE).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(APPROVAL_ZONE).toInstant();
        if (jobs.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end) >= DAILY_LIMIT) {
            throw new ApprovalRejected("오늘의 승인 한도 10건을 모두 사용했습니다.");
        }
        CommentJob job = jobs.save(new CommentJob(videoId, profile.strip(), text, now));
        events.save(new CommentJobEvent(job, JobStatus.APPROVED, now, null, null));
    }
}
