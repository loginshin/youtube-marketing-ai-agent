package com.marketing.agent.analysis.presentation;

import com.marketing.agent.analysis.application.VideoAnalysisSettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis/video/settings")
public class VideoAnalysisSettingsController {
    private final VideoAnalysisSettingsService settings;

    public VideoAnalysisSettingsController(VideoAnalysisSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public VideoAnalysisSettingsService.Settings current() { return settings.current(); }

    @PutMapping
    public VideoAnalysisSettingsService.Settings save(@RequestBody VideoAnalysisSettingsService.Update update) {
        return settings.save(update);
    }
}
