package com.marketing.agent.comment.publication;

import com.marketing.agent.db.entity.CommentJob;
import com.marketing.agent.video.infrastructure.YouTubeClient.PublishedSnippet;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "posted_comment")
public class PostedComment {
    @Id public String commentId;
    public Long jobId;
    public String videoId;
    public String videoTitle;
    public String videoUrl;
    public String commentUrl;
    public String profileName;
    public String authorName;
    public String authorChannelId;
    public String body;
    public Instant publishedAt;
    public Instant verifiedAt;

    protected PostedComment() {}

    PostedComment(CommentJob job, String title, String id, PublishedSnippet snippet) {
        commentId = id;
        jobId = job.getId();
        videoId = job.getVideoId();
        videoTitle = title;
        videoUrl = "https://www.youtube.com/watch?v=" + videoId;
        commentUrl = videoUrl + "&lc=" + id;
        profileName = job.getProfileName();
        authorName = snippet.authorDisplayName();
        authorChannelId = snippet.authorChannelId().value();
        body = snippet.textDisplay();
        publishedAt = snippet.publishedAt();
        verifiedAt = Instant.now();
    }
}
