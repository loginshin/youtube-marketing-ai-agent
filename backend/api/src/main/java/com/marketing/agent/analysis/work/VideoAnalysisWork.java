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
    private final AnalysisCompletion completion;
    private final com.marketing.agent.ai.GeminiVideoService videoAnalyzer;
    private final com.marketing.agent.analysis.application.VideoAnalysisSettingsService videoSettings;
    private final DraftService drafts;
    private final CommentPromptService settings;
    private final ExecutorService workers = Executors.newFixedThreadPool(3);

    VideoAnalysisWork(VideoAnalysisRepository jobs, VideoSummaryService summaries, DraftService drafts, CommentPromptService settings, AnalysisCompletion completion, com.marketing.agent.ai.GeminiVideoService videoAnalyzer, com.marketing.agent.analysis.application.VideoAnalysisSettingsService videoSettings) {
        this.videoSettings = videoSettings;
        this.videoAnalyzer = videoAnalyzer;
        this.jobs = jobs;
        this.completion = completion;
        this.summaries = summaries;
        this.drafts = drafts;
        this.settings = settings;
    }

    @Override public void run(ApplicationArguments args) {
        // This local server owns the workers. An interrupted paid call requires explicit retry.
        for (var job : jobs.findAllByStageIn(List.of(SUMMARY, DRAFT))) {
            job.failureReason = "서버가 재시작되어 분석이 중단되었습니다. 다시 시도해주세요.";
            job.stage = FAILED;
            completion.finish(job);
        }
    }

    @PreDestroy void stop() { workers.shutdownNow(); }

    synchronized VideoAnalysis.Snapshot start(String id, Input input) {
        var existing = jobs.findById(id);
        if (existing.isPresent()) return existing.get().snapshot();
        return submit(new VideoAnalysis(id), input);
    }

    synchronized VideoAnalysis.Snapshot retry(String id, Input input, String previousAttempt, String evidence) {
        if (Boolean.TRUE.equals(input.analyzeVideo())) videoAnalyzer.requireConfigured();
        var job = get(id);
        if (job.running() || !job.attempt.equals(previousAttempt)) return job.snapshot();
        job.stage = Boolean.TRUE.equals(input.analyzeVideo()) || evidence.isBlank() ? SUMMARY : DRAFT;
        job.evidence = evidence;
        job.attempt = java.util.UUID.randomUUID().toString();
        return submit(job, input);
    }

    VideoAnalysis get(String id) {
        return jobs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private VideoAnalysis.Snapshot submit(VideoAnalysis job, Input input) {
        job.startedAt = java.time.Instant.now();
        job.failureReason = null;
        job.analysisType = Boolean.TRUE.equals(input.analyzeVideo()) ? "VIDEO" : "CONTENT";
        var saved = jobs.saveAndFlush(job);
        var snapshot = saved.snapshot();
        workers.execute(() -> execute(saved, input));
        return snapshot;
    }

    private void execute(VideoAnalysis job, Input input) {
        try {
            var config = settings.current();
            if (job.stage == SUMMARY) {
                var videoConfig = videoSettings.current();
                var summary = Boolean.TRUE.equals(input.analyzeVideo()) ? videoAnalyzer.analyze(job.videoId, videoConfig.modelTier(), videoConfig.instructions())
                        : summaries.summarize(input.title(), input.description(), input.comments(), config.modelTier());
                if (!"ready".equals(summary.status())) {
                    job.stage = INSUFFICIENT;
                    completion.finish(job);
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
            org.slf4j.LoggerFactory.getLogger(VideoAnalysisWork.class)
                    .warn("Analysis failed for video {} at stage {} ({})", job.videoId, job.stage, exception.getClass().getSimpleName());
            job.failureReason = failureReason(exception, job.stage.name());
            job.stage = FAILED;
        }
        completion.finish(job);
    }

    private String failureReason(RuntimeException exception, String stage) {
        String prefix = stage.equals("SUMMARY") ? "분석 실패 · " : "댓글 초안 생성 실패 · ";
        if (exception instanceof org.springframework.web.client.RestClientResponseException response) {
            return prefix + switch (response.getStatusCode().value()) {
                case 400 -> "요청 형식 또는 영상 접근 조건을 확인해주세요. (HTTP 400)";
                case 401, 403 -> "API 키와 API 사용 권한을 확인해주세요. (HTTP " + response.getStatusCode().value() + ")";
                case 404 -> "요청한 모델 또는 영상을 사용할 수 없습니다. AI 설정을 확인해주세요. (HTTP 404)";
                case 429 -> "API 사용 한도 또는 크레딧을 확인해주세요. (HTTP 429)";
                default -> "AI 서버 오류입니다. 잠시 후 다시 시도해주세요. (HTTP " + response.getStatusCode().value() + ")";
            };
        }
        if (exception instanceof org.springframework.web.client.ResourceAccessException) {
            return prefix + "AI 응답 대기 시간이 초과되었거나 네트워크 연결에 실패했습니다.";
        }
        return prefix + "AI 응답 처리에 실패했습니다. 응답 형식과 초안 길이를 확인해주세요.";
    }

    record Input(String title, String description, List<String> comments, Boolean analyzeVideo) {
        Input(String title, String description, List<String> comments) {
            this(title, description, comments, false);
        }
        @com.fasterxml.jackson.annotation.JsonCreator
        Input {
            if (title == null || title.isBlank() || title.length() > 300 || description == null ||
                description.length() > 10000 || comments == null || comments.size() > 5 ||
                comments.stream().anyMatch(c -> c == null || c.length() > 10000)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        }
    }
}
