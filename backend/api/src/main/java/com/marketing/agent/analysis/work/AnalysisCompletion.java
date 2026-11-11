package com.marketing.agent.analysis.work;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AnalysisCompletion {
    private final VideoAnalysisRepository jobs;
    private final AnalysisRevisionRepository revisions;

    AnalysisCompletion(VideoAnalysisRepository jobs, AnalysisRevisionRepository revisions) {
        this.jobs = jobs;
        this.revisions = revisions;
    }

    @Transactional
    public void finish(VideoAnalysis job) {
        jobs.save(job);
        revisions.save(new AnalysisRevision(job));
    }
}
