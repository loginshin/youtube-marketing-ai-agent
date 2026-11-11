package com.marketing.agent.analysis.work;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface VideoAnalysisRepository extends JpaRepository<VideoAnalysis, String> {
    List<VideoAnalysis> findAllByStageIn(List<VideoAnalysis.Stage> stages);
}
