package com.marketing.agent.analysis.presentation;

import com.marketing.agent.ai.DraftService;
import com.marketing.agent.ai.VideoSummaryService;
import java.util.List;
import com.marketing.agent.analysis.application.CommentPromptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {
    private final DraftService drafts;

    private final VideoSummaryService summaries;

    private final CommentPromptService prompts;

    public AnalysisController(DraftService drafts, VideoSummaryService summaries, CommentPromptService prompts) {
        this.drafts = drafts;
        this.summaries = summaries;
        this.prompts = prompts;
    }

    @GetMapping("/draft/prompt")
    public CommentPromptService.Settings prompt() {
        return prompts.current();
    }

    @PutMapping("/draft/prompt")
    public CommentPromptService.Settings savePrompt(@RequestBody CommentPromptService.Update settings) {
        return prompts.save(settings);
    }

    @PostMapping("/draft")
    public DraftResponse generate(@RequestBody DraftRequest request) {
        if (request.title() == null || request.title().isBlank() || request.title().length() > 300
                || request.evidence() == null || request.evidence().isBlank()
                || request.evidence().length() > 10000) {
            throw new IllegalArgumentException("제목(1~300자)과 확인 가능한 근거(1~10,000자)가 필요합니다.");
        }
        var settings = prompts.current();
        return new DraftResponse(drafts.generate(request.title(), request.evidence(), settings.instructions(), settings.modelTier()));
    }

    @PostMapping("/summary")
    public VideoSummaryService.Result summarize(@RequestBody SummaryRequest request) {
        return summaries.summarize(request.title(), request.description(), request.comments(), prompts.current().modelTier());
    }

    public record SummaryRequest(String title, String description, List<String> comments) {}
    public record DraftRequest(String title, String evidence) {}
    public record DraftResponse(String draft) {}
}
