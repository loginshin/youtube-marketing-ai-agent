package com.marketing.agent.analysis.application;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.Repository;

public interface VideoAnalysisSettingsRepository extends Repository<VideoAnalysisSettings, Integer> {
    VideoAnalysisSettings getById(Integer id);

    @Modifying
    @Query("""
            update VideoAnalysisSettings s set s.instructions = :instructions, s.model = :model,
            s.version = s.version + 1 where s.id = 1 and s.version = :version
            """)
    int updateIfCurrent(String instructions, String model, long version);
}
