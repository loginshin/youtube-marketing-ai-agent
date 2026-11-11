package com.marketing.agent.analysis.work;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/analysis/videos/{id:[a-zA-Z0-9_-]{11}}")
class VideoAnalysisController {
    private final VideoAnalysisWork work;
    private final AnalysisRevisionRepository revisions;

    VideoAnalysisController(VideoAnalysisWork work, AnalysisRevisionRepository revisions) {
        this.work = work;
        this.revisions = revisions;
    }

    @GetMapping("/history")
    java.util.List<AnalysisRevisionRepository.Entry> history(@PathVariable String id) {
        return revisions.history(id);
    }

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

    @PutMapping("/history/{attempt}/screenshot")
    @org.springframework.transaction.annotation.Transactional
    void screenshot(@PathVariable String id, @PathVariable String attempt, @RequestBody Capture capture) {
        var revision = revisions.findById(attempt).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!revision.videoId.equals(id) || !"FAILED".equals(revision.outcome))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        String image = capture.image();
        if (image == null || image.length() > 2_000_000 || !image.startsWith("data:image/png;base64,"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        byte[] bytes;
        try { bytes = java.util.Base64.getDecoder().decode(image.substring(22)); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
        if (bytes.length < 8 || bytes[0] != (byte) 137 || bytes[1] != 80 || bytes[2] != 78 || bytes[3] != 71)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (revision.screenshot == null) revision.screenshot = image;
    }

    @GetMapping("/history/{attempt}/screenshot")
    Capture screenshot(@PathVariable String id, @PathVariable String attempt) {
        var revision = revisions.findById(attempt).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!revision.videoId.equals(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return new Capture(revision.screenshot);
    }

    record Capture(String image) {}
}
