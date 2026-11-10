package com.marketing.agent.comment;

import com.marketing.agent.comment.application.ApprovalRejected;
import com.marketing.agent.comment.application.ApprovalService;
import com.marketing.agent.comment.application.JobHistoryService;
import com.marketing.agent.db.entity.JobStatus;
import com.marketing.agent.db.repository.CommentJobEventRepository;
import com.marketing.agent.db.repository.CommentJobRepository;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:approval-concurrency")
class ApprovalConcurrencyTests {
    private static final String BODY = "검".repeat(40);
    @Autowired ApprovalService approvals;
    @Autowired JobHistoryService history;
    @Autowired CommentJobRepository jobs;
    @Autowired CommentJobEventRepository events;

    @BeforeEach
    void clearIsolatedDatabase() {
        events.deleteAllInBatch();
        jobs.deleteAllInBatch();
    }

    @Test
    void twoProfilesProduceOneApprovalAndOneEventForTheSameVideo() throws Exception {
        assertEquals(1, concurrentApprovals("video000001", "video000001"));
        var stored = history.find("video000001").orElseThrow();
        assertEquals(BODY, stored.body());
        assertEquals(JobStatus.APPROVED, stored.status());
        assertEquals(1, stored.events().size());
        assertEquals(JobStatus.APPROVED, stored.events().getFirst().status());
        assertEquals(1, jobs.count());
        assertEquals(1, events.count());
    }

    @Test
    void onlyOneVideoCanTakeTheLastDailySlot() throws Exception {
        for (int i = 0; i < 9; i++) {
            approvals.approve("seed000000" + i, "유튜브 1", BODY);
        }
        assertEquals(1, concurrentApprovals("video000001", "video000002"));
        assertEquals(10, jobs.count());
        assertEquals(10, events.count());
    }

    private int concurrentApprovals(String first, String second) throws Exception {
        var start = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> attempt(start, first, "유튜브 1"));
            var b = pool.submit(() -> attempt(start, second, "유튜브 2"));
            return a.get(10, TimeUnit.SECONDS) + b.get(10, TimeUnit.SECONDS);
        }
    }

    private int attempt(CyclicBarrier start, String id, String profile) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try {
            approvals.approve(id, profile, BODY);
            return 1;
        } catch (ApprovalRejected rejected) {
            return 0;
        }
    }
}
