package com.marketing.agent.browser;

import com.marketing.agent.comment.publication.*;
import com.marketing.agent.db.entity.JobStatus;
import com.marketing.agent.video.infrastructure.YouTubeClient;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
class PublicationController {
    private final PublicationStore store;
    private final PostedCommentRepository history;
    private final ChromePreparation chrome;
    private final YouTubeClient youtube;

    PublicationController(PublicationStore store, PostedCommentRepository history, ChromePreparation chrome, YouTubeClient youtube) {
        this.store = store;
        this.history = history;
        this.chrome = chrome;
        this.youtube = youtube;
    }

    @GetMapping("/posted-comments")
    List<PostedComment> recent() {
        return history.findAll(PageRequest.of(0, 100, Sort.by("publishedAt", "commentId").descending())).getContent();
    }

    @GetMapping(value = "/publication-failures/{file:[a-f0-9-]+\\.png}", produces = "image/png")
    org.springframework.core.io.Resource failureScreenshot(@PathVariable String file) {
        if (!file.matches("[a-f0-9-]{36}\\.png")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var path = java.nio.file.Path.of(System.getProperty("user.home"), ".clipdesk", "publication-failures", file);
        if (!java.nio.file.Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return new org.springframework.core.io.FileSystemResource(path);
    }

    @GetMapping("/videos/{id:[a-zA-Z0-9_-]{11}}/job/progress")
    org.springframework.http.ResponseEntity<ChromePreparation.Progress> progress(@PathVariable String id) {
        var progress = chrome.progress(id);
        return progress == null ? org.springframework.http.ResponseEntity.noContent().build()
                : org.springframework.http.ResponseEntity.ok(progress);
    }

    @PostMapping(value = "/videos/{id:[a-zA-Z0-9_-]{11}}/job/publish", consumes = "application/json")
    ChromePreparation.Result publish(@PathVariable String id) {
        var job = store.job(id);
        if (job.getStatus() != JobStatus.APPROVED || !"유튜브 1".equals(job.getProfileName()))
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        try {
            var title = title(id);
            var commentId = chrome.publish(id, job.getBody(), () -> store.reserve(id),
                    comment -> store.remember(id, comment),
                    (reason, image) -> store.recordFailure(id, reason, image)).join();
            if (commentId != null) {
                verify(id, title, commentId);
                return new ChromePreparation.Result("COMPLETED", "게시 완료 · 댓글 보기", "https://www.youtube.com/watch?v=" + id + "&lc=" + commentId);
            }
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof ResponseStatusException rejected) throw rejected;
            if (ex.getCause() instanceof IllegalStateException known)
                return new ChromePreparation.Result("CHECK_REQUIRED", known.getMessage());
        } catch (IllegalStateException busy) {
            return new ChromePreparation.Result("CHECK_REQUIRED", busy.getMessage());
        } catch (org.springframework.web.client.RestClientException | ResponseStatusException ex) {
            // The receipt may not be publicly visible yet.
        }
        boolean pending = store.job(id).getStatus() == JobStatus.CONFIRMATION_PENDING;
        return new ChromePreparation.Result(pending ? "PENDING" : "UNAVAILABLE", pending
                ? "게시 여부를 확인 중입니다. 댓글 링크로 결과를 다시 확인하세요."
                : "게시를 시작하지 못했습니다. Chrome과 YouTube 연결을 확인하세요.");
    }

    @PostMapping("/videos/{id:[a-zA-Z0-9_-]{11}}/job/verify")
    PostedComment verifyLink(@PathVariable String id, @RequestBody Link request) {
        String receipt = store.job(id).getPendingCommentId();
        String candidate = request.url() == null || request.url().isBlank() ? receipt : commentId(request.url());
        if (candidate == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return verify(id, title(id), candidate);
    }

    private String title(String id) {
        var video = youtube.video(id);
        if (video == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return video.snippet().title();
    }

    private PostedComment verify(String videoId, String title, String commentId) {
        var snippet = youtube.publishedComment(commentId);
        if (snippet == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return store.confirm(videoId, title, commentId, snippet);
    }

    static String commentId(String link) {
        try {
            var url = URI.create(link.strip());
            if ("https".equals(url.getScheme()) && url.getHost() != null &&
                    List.of("www.youtube.com", "youtube.com", "m.youtube.com", "youtu.be").contains(url.getHost())
                    && url.getQuery() != null)
                return Arrays.stream(url.getQuery().split("&")).filter(s -> s.matches("lc=[A-Za-z0-9_-]{1,256}"))
                        .map(s -> s.substring(3)).findFirst().orElseThrow();
        } catch (IllegalArgumentException | java.util.NoSuchElementException ignored) { }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    record Link(String url) {}
}
