package com.marketing.agent.comment.publication;

import com.marketing.agent.comment.application.ApprovalService;
import com.marketing.agent.db.entity.JobStatus;
import com.marketing.agent.video.infrastructure.YouTubeClient.ChannelId;
import com.marketing.agent.video.infrastructure.YouTubeClient.PublishedSnippet;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:publication")
class PublicationStoreTests {
    @Autowired PublicationStore store;
    @Autowired ApprovalService approvals;
    @Autowired PostedCommentRepository records;

    @Test
    void recordsVerifiedReceiptOnceAndNeverResubmitsUncertainClick() {
        String video = "abcdefghijk";
        String body = "가".repeat(40);
        approvals.approve(video, "유튜브 1", body);
        store.reserve(video);
        assertThrows(ResponseStatusException.class, () -> store.reserve(video));
        var when = Instant.parse("2026-01-01T00:00:00Z");
        var wrong = new PublishedSnippet(video, null, "작성자", new ChannelId("channel"), "다른 댓글", when);
        assertThrows(ResponseStatusException.class, () -> store.confirm(video, "영상 제목", "comment1", wrong));
        assertEquals(0, records.count());
        assertEquals(JobStatus.CONFIRMATION_PENDING, store.job(video).getStatus());
        store.remember(video, "comment1");
        var verified = new PublishedSnippet(video, null, "작성자", new ChannelId("channel"), body, when);
        var receipt = store.confirm(video, "영상 제목", "comment1", verified);
        store.confirm(video, "영상 제목", "comment1", verified);
        assertEquals(1, records.count());
        assertEquals(JobStatus.COMPLETED, store.job(video).getStatus());
        assertEquals(when, receipt.publishedAt);
        assertEquals("channel", receipt.authorChannelId);
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk&lc=comment1", receipt.commentUrl);
    }
}
