package com.marketing.agent.analysis.presentation;

import com.marketing.agent.ai.DraftService;
import com.marketing.agent.ai.VideoSummaryService;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {
    private final DraftService drafts;

    private final VideoSummaryService summaries;

    public AnalysisController(DraftService drafts, VideoSummaryService summaries) {
        this.drafts = drafts;
        this.summaries = summaries;
    }

    @PostMapping("/draft")
    public DraftResponse generate(@RequestBody DraftRequest request) {
        if (request.title() == null || request.title().isBlank() || request.title().length() > 300
                || request.evidence() == null || request.evidence().isBlank()
                || request.evidence().length() > 10000) {
            throw new IllegalArgumentException("제목(1~300자)과 확인 가능한 근거(1~10,000자)가 필요합니다.");
        }
        return new DraftResponse(drafts.generate(request.title(), request.evidence()));
    }

    @PostMapping("/summary")
    public VideoSummaryService.Result summarize(@RequestBody SummaryRequest request) {
        return summaries.summarize(request.title(), request.description(), request.comments());
    }

    public record SummaryRequest(String title, String description, List<String> comments) {}
    public record DraftRequest(String title, String evidence) {}
    public record DraftResponse(String draft) {}
}
