package com.marketing.agent.video.infrastructure;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class YouTubeClient {
    private final RestClient client;
    private final String key;

    public YouTubeClient(@Value("${agent.youtube.api-key:}") String key) {
        this.key = key;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        client = RestClient.builder().baseUrl("https://www.googleapis.com/youtube/v3")
                .requestFactory(factory).build();
    }

    public List<SearchItem> search(String query) {
        requireKey();
        SearchResponse response = client.get().uri(builder -> builder.path("/search")
                .queryParam("part", "snippet").queryParam("type", "video")
                .queryParam("order", "date").queryParam("maxResults", 10)
                .queryParam("q", query).build()).header("X-Goog-Api-Key", key)
                .retrieve().body(SearchResponse.class);
        return response == null || response.items() == null ? List.of() : response.items();
    }

    public VideoItem video(String id) {
        requireKey();
        VideoResponse response = client.get().uri(builder -> builder.path("/videos")
                .queryParam("part", "snippet").queryParam("id", id).build())
                .header("X-Goog-Api-Key", key).retrieve().body(VideoResponse.class);
        return response == null || response.items() == null || response.items().isEmpty()
                ? null : response.items().getFirst();
    }

    public Comments comments(String id) {
        requireKey();
        try {
            CommentResponse response = client.get().uri(builder -> builder.path("/commentThreads")
                    .queryParam("part", "snippet").queryParam("videoId", id)
                    .queryParam("order", "relevance").queryParam("textFormat", "plainText")
                    .queryParam("maxResults", 5).build()).header("X-Goog-Api-Key", key)
                    .retrieve().body(CommentResponse.class);
            List<String> texts = response == null || response.items() == null ? List.of()
                    : response.items().stream().map(item -> item.snippet().topLevelComment().snippet().textDisplay()).toList();
            return new Comments(texts, null);
        } catch (RestClientResponseException exception) {
            // Only commentsDisabled is a partial success. Other errors reach the API error handler.
            if (exception.getStatusCode().value() == 403
                    && exception.getResponseBodyAsString().contains("\"commentsDisabled\"")) {
                return new Comments(List.of(), "댓글이 비활성화되어 설명만 불러왔습니다.");
            }
            throw exception;
        }
    }

    private void requireKey() {
        if (key.isBlank()) throw new IllegalStateException("backend/.env에 YOUTUBE_API_KEY를 설정하세요.");
    }

    public record Thumbnail(String url) {}
    public record Snippet(String title, String channelTitle, String publishedAt, String description,
                          Map<String, Thumbnail> thumbnails) {
        public String thumbnailUrl() {
            if (thumbnails == null) return null;
            for (String size : List.of("medium", "default", "high", "standard", "maxres")) {
                Thumbnail image = thumbnails.get(size);
                if (image != null && image.url() != null && !image.url().isBlank()) return image.url();
            }
            return null;
        }
    }
    public record SearchId(String videoId) {}
    public record SearchItem(SearchId id, Snippet snippet) {}
    public record SearchResponse(List<SearchItem> items) {}
    public record VideoItem(String id, Snippet snippet) {}
    public record VideoResponse(List<VideoItem> items) {}
    public record CommentSnippet(String textDisplay) {}
    public record TopComment(CommentSnippet snippet) {}
    public record ThreadSnippet(TopComment topLevelComment) {}
    public record ThreadItem(ThreadSnippet snippet) {}
    public record CommentResponse(List<ThreadItem> items) {}
    public record Comments(List<String> texts, String warning) {}
}
