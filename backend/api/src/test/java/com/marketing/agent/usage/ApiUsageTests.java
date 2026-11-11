package com.marketing.agent.usage;

import com.marketing.agent.ai.OpenAiClient;
import com.marketing.agent.ai.OpenAiUsage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:usage")
class ApiUsageTests {
    @Autowired ApplicationEventPublisher events;
    @Autowired ApiUsageController controller;
    @Autowired ApiUsageRecorder recorder;
    @Autowired JsonMapper json;

    @Test
    void aggregatesReportedTokensSeparatelyFromUnmeasuredCalls() {
        var response = json.readValue("""
                {"status":"incomplete","usage":{"input_tokens":23,"output_tokens":8,"total_tokens":31}}
                """, OpenAiClient.Response.class);
        events.publishEvent(new OpenAiUsage("test-model", response.usage().inputTokens(), response.usage().outputTokens()));
        events.publishEvent(new OpenAiUsage("test-model", null, null));
        recorder.recordYouTube("search.list");
        var rows = controller.summary(1);
        assertEquals(2, rows.size());
        assertEquals(new ApiUsageRepository.Summary("OpenAI", "test-model", 2, 1, 23, 8), rows.getFirst());
        assertEquals(new ApiUsageRepository.Summary("YouTube", "search.list", 1, 0, 0, 0), rows.getLast());
    }
}
