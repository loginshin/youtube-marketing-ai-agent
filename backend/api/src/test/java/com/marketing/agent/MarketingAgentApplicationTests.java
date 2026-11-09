package com.marketing.agent;

import com.marketing.agent.db.entity.CommentJob;
import com.marketing.agent.db.entity.CommentJobEvent;
import com.marketing.agent.db.entity.JobStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:agent-test")
class MarketingAgentApplicationTests {
    @Autowired EntityManager entities;

    @Test
    @Transactional
    void savesJobAndHistoryUsingSqlSchema() {
        Instant now = Instant.now();
        CommentJob job = new CommentJob("video-id", "test-profile", "검토를 마친 댓글 초안", now);
        entities.persist(job);
        CommentJobEvent event = new CommentJobEvent(job, JobStatus.APPROVED, now, null, null);
        entities.persist(event);
        entities.flush();
        entities.clear();
        assertNotNull(entities.find(CommentJobEvent.class, event.getId()));
    }
}
