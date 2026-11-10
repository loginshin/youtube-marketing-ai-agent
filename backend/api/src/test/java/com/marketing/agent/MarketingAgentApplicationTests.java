package com.marketing.agent;

import com.marketing.agent.db.entity.CommentJob;
import com.marketing.agent.db.entity.CommentJobEvent;
import com.marketing.agent.db.entity.JobStatus;
import com.marketing.agent.db.repository.CommentJobRepository;
import com.marketing.agent.db.repository.CommentJobEventRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:agent-test")
class MarketingAgentApplicationTests {
    @Autowired EntityManager entities;
    @Autowired CommentJobRepository jobs;
    @Autowired CommentJobEventRepository events;

    @Test
    @Transactional
    void savesJobAndHistoryUsingSqlSchema() {
        Instant now = Instant.now();
        CommentJob job = new CommentJob("video-id", "test-profile", "검토를 마친 댓글 초안", now);
        jobs.save(job);
        CommentJobEvent event = new CommentJobEvent(job, JobStatus.APPROVED, now, null, null);
        events.saveAndFlush(event);
        entities.clear();
        CommentJobEvent stored = events.findById(event.getId()).orElseThrow();
        assertEquals(event.getId(), stored.getId());
    }
}
