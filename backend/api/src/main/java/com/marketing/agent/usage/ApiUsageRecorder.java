package com.marketing.agent.usage;

import com.marketing.agent.ai.OpenAiUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApiUsageRecorder {
    private static final Logger log = LoggerFactory.getLogger(ApiUsageRecorder.class);
    private final ApiUsageRepository repository;

    public ApiUsageRecorder(ApiUsageRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void recordOpenAi(OpenAiUsage usage) {
        save(new ApiUsageEvent("OpenAI", usage.model(), usage.inputTokens(), usage.outputTokens()));
    }

    public void recordYouTube(String operation) {
        save(new ApiUsageEvent("YouTube", operation, null, null));
    }

    private void save(ApiUsageEvent event) {
        try {
            repository.saveAndFlush(event);
        } catch (org.springframework.dao.DataAccessException exception) {
            log.warn("API 사용량 기록 실패", exception);
        }
    }
}
