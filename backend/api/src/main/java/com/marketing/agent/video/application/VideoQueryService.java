package com.marketing.agent.video.application;

import com.marketing.agent.video.infrastructure.YouTubeClient;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VideoQueryService {
    private final YouTubeClient youtube;

    public VideoQueryService(YouTubeClient youtube) {
        this.youtube = youtube;
    }

    public List<VideoSummary> search(String query) {
        if (query.isBlank() || query.length() > 100) {
            throw new IllegalArgumentException("검색어는 1~100자로 입력하세요.");
        }
        return youtube.search(query.strip()).stream().map(item -> new VideoSummary(
                item.id().videoId(), item.snippet().title(), item.snippet().channelTitle(),
                item.snippet().publishedAt(), item.snippet().thumbnailUrl())).toList();
    }

    public VideoDetail detail(String id) {
        if (!id.matches("[A-Za-z0-9_-]{11}")) {
            throw new IllegalArgumentException("올바른 YouTube 영상 ID가 필요합니다.");
        }
        var video = youtube.video(id);
        if (video == null) throw new VideoNotFoundException();
        var comments = youtube.comments(id);
        return new VideoDetail(video.id(), video.snippet().title(), video.snippet().channelTitle(),
                video.snippet().publishedAt(), video.snippet().description(), comments.texts(), comments.warning());
    }

    public record VideoSummary(String id, String title, String channel, String date, String thumbnailUrl) {}
    public record VideoDetail(String id, String title, String channel, String date,
                              String description, List<String> comments, String warning) {}
}
