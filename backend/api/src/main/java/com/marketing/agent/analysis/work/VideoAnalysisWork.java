package com.marketing.agent.analysis.work;

import com.marketing.agent.ai.DraftService;
import com.marketing.agent.ai.VideoSummaryService;
import com.marketing.agent.analysis.application.CommentPromptService;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static com.marketing.agent.analysis.work.VideoAnalysis.Stage.*;

@Service
class VideoAnalysisWork implements ApplicationRunner {
    private final VideoAnalysisRepository jobs;
    private final VideoSummaryService summaries;
    private final DraftService drafts;
    private final CommentPromptService settings;
    private final ExecutorService workers = Executors.newFixedThreadPool(3);

    VideoAnalysisWork(VideoAnalysisRepository jobs, VideoSummaryService summaries, DraftService drafts, CommentPromptService settings) {
        this.jobs = jobs;
        this.summaries = summaries;
        this.drafts = drafts;
        this.settings = settings;
    }

    @Override public void run(ApplicationArguments args) {
        // This local server owns the workers. An interrupted paid call requires explicit retry.
        for (var job : jobs.findAllByStageIn(List.of(SUMMARY, DRAFT))) {
            job.stage = FAILED;
            jobs.save(job);
        }
    }

    @PreDestroy void stop() { workers.shutdownNow(); }

    synchronized VideoAnalysis.Snapshot start(String id, Input input) {
        var existing = jobs.findById(id);
        if (existing.isPresent()) return existing.get().snapshot();
        return submit(new VideoAnalysis(id), input);
    }

    synchronized VideoAnalysis.Snapshot retry(String id, Input input, String previousAttempt, String evidence) {
        var job = get(id);
        if (job.running() || !job.attempt.equals(previousAttempt)) return job.snapshot();
        job.stage = evidence.isBlank() ? SUMMARY : DRAFT;
        job.evidence = evidence;
        job.attempt = java.util.UUID.randomUUID().toString();
        return submit(job, input);
    }

    VideoAnalysis get(String id) {
        return jobs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private VideoAnalysis.Snapshot submit(VideoAnalysis job, Input input) {
        var saved = jobs.saveAndFlush(job);
        var snapshot = saved.snapshot();
        workers.execute(() -> execute(saved, input));
        return snapshot;
    }

    private void execute(VideoAnalysis job, Input input) {
        try {
            var config = settings.current();
            if (job.stage == SUMMARY) {
                var summary = summaries.summarize(input.title(), input.description(), input.comments(), config.modelTier());
                if (!"ready".equals(summary.status())) {
                    job.stage = INSUFFICIENT;
                    jobs.save(job);
                    return;
                }
                job.concern = summary.summary();
                job.evidence = summary.source() + " · " + summary.quote();
                job.stage = DRAFT;
                job = jobs.saveAndFlush(job);
            }
            String evidence = "고민 요약: " + job.concern + "\n출처와 인용: " + job.evidence;
            job.draft = drafts.generate(input.title(), evidence, config.instructions(), config.modelTier());
            job.stage = READY;
        } catch (RuntimeException exception) {
            job.stage = FAILED;
        }
        jobs.saveAndFlush(job);
    }

    record Input(String title, String description, List<String> comments) {
        Input {
            if (title == null || title.isBlank() || title.length() > 300 || description == null ||
                description.length() > 10000 || comments == null || comments.size() > 5 ||
                comments.stream().anyMatch(c -> c == null || c.length() > 10000)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        }
    }
}
