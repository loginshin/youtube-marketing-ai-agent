package com.marketing.agent.video.presentation;

import com.marketing.agent.video.infrastructure.YouTubeClient;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/videos")
public class VideoController {
    private final YouTubeClient youtube;

    public VideoController(YouTubeClient youtube) { this.youtube = youtube; }

    @GetMapping
    public List<VideoSummary> search(@RequestParam(defaultValue = "연애") String query) {
        if (query.isBlank() || query.length() > 100) {
            throw new IllegalArgumentException("검색어는 1~100자로 입력하세요.");
        }
        return youtube.search(query.strip()).stream().map(item -> new VideoSummary(
                item.id().videoId(), item.snippet().title(), item.snippet().channelTitle(),
                item.snippet().publishedAt())).toList();
    }

    @GetMapping("/{id}")
    public VideoDetail detail(@PathVariable String id) {
        if (!id.matches("[A-Za-z0-9_-]{11}")) {
            throw new IllegalArgumentException("올바른 YouTube 영상 ID가 필요합니다.");
        }
        var video = youtube.video(id);
        if (video == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "영상을 찾을 수 없습니다.");
        var comments = youtube.comments(id);
        return new VideoDetail(video.id(), video.snippet().title(), video.snippet().channelTitle(),
                video.snippet().publishedAt(), video.snippet().description(), comments.texts(), comments.warning());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorResponse> missingKey(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<ErrorResponse> upstreamFailure(RestClientException exception) {
        if (exception instanceof RestClientResponseException response
                && (response.getStatusCode().value() == 403 || response.getStatusCode().value() == 429)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(
                    "YouTube 요청이 제한되었습니다. 키·API 활성화·키 제한·할당량을 확인하세요."));
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(
                "YouTube 조회에 실패했습니다. 네트워크와 API 설정을 확인하고 다시 시도하세요."));
    }

    public record VideoSummary(String id, String title, String channel, String date) {}
    public record VideoDetail(String id, String title, String channel, String date,
                              String description, List<String> comments, String warning) {}
    public record ErrorResponse(String message) {}
}
