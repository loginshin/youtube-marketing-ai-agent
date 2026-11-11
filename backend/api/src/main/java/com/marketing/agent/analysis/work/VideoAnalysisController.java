package com.marketing.agent.analysis.work;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/analysis/videos/{id:[a-zA-Z0-9_-]{11}}")
class VideoAnalysisController {
    private final VideoAnalysisWork work;
    VideoAnalysisController(VideoAnalysisWork work) { this.work = work; }

    @GetMapping VideoAnalysis.Snapshot current(@PathVariable String id) { return work.get(id).snapshot(); }

    @PostMapping VideoAnalysis.Snapshot start(@PathVariable String id, @RequestBody VideoAnalysisWork.Input input) {
        return work.start(id, input);
    }

    @PostMapping("/retry") VideoAnalysis.Snapshot retry(@PathVariable String id, @RequestBody Retry request) {
        if (request.input() == null || request.attempt() == null || request.evidence() == null || request.evidence().length() > 10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return work.retry(id, request.input(), request.attempt(), request.evidence().strip());
    }

    record Retry(VideoAnalysisWork.Input input, String attempt, String evidence) {}
}
