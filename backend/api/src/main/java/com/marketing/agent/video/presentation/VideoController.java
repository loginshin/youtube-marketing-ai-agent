package com.marketing.agent.video.presentation;

import com.marketing.agent.video.application.VideoQueryService;
import com.marketing.agent.video.application.VideoQueryService.VideoDetail;
import com.marketing.agent.video.application.VideoQueryService.VideoSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
public class VideoController {
    private final VideoQueryService videos;

    public VideoController(VideoQueryService videos) {
        this.videos = videos;
    }

    @GetMapping
    public List<VideoSummary> search(@RequestParam(defaultValue = "연애") String query) {
        return videos.search(query);
    }

    @GetMapping("/{id}")
    public VideoDetail detail(@PathVariable String id) {
        return videos.detail(id);
    }
}
