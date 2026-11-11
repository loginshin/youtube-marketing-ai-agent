package com.marketing.agent.browser;

import com.marketing.agent.db.entity.JobStatus;
import com.marketing.agent.db.repository.CommentJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/videos/{videoId:[a-zA-Z0-9_-]{11}}/job")
class ChromePreparationController {
    private final CommentJobRepository jobs;
    private final ChromePreparation chrome;

    ChromePreparationController(CommentJobRepository jobs, ChromePreparation chrome) {
        this.jobs = jobs;
        this.chrome = chrome;
    }

    @PostMapping(value = "/prepare", consumes = "application/json")
    ChromePreparation.Result prepare(@PathVariable String videoId) {
        var job = jobs.findByVideoId(videoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (job.getStatus() != JobStatus.APPROVED || !"유튜브 1".equals(job.getProfileName()))
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        return chrome.prepare(job.getVideoId()).join();
    }
}
