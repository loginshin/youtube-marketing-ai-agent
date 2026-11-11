package com.marketing.agent.analysis.work;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AnalysisRevisionRepository extends JpaRepository<AnalysisRevision, String> {
    record Entry(String attempt, Instant recordedAt, String outcome, String evidence, String draft) {}

    @Query("""
            select new com.marketing.agent.analysis.work.AnalysisRevisionRepository$Entry(
                r.attempt, r.recordedAt, r.outcome, r.evidence, r.draft)
            from AnalysisRevision r where r.videoId = :id
            order by r.recordedAt desc nulls last, r.attempt
            """)
    List<Entry> history(String id);
}
