package com.marketing.agent.analysis;

import com.marketing.agent.ai.ModelTier;
import com.marketing.agent.ai.OpenAiClient;
import java.util.List;
import com.marketing.agent.analysis.application.CommentPromptService;
import com.marketing.agent.analysis.presentation.AnalysisController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:comment-prompt-test")
class CommentPromptTests {
    @Autowired com.marketing.agent.analysis.application.VideoAnalysisSettingsService videoSettings;
    @Autowired AnalysisController analysis;
    @MockitoBean OpenAiClient ai;

    @org.junit.jupiter.api.Test
    void videoSettingsArePersistedIndependentlyAndRejectStaleUpdates() {
        var contentSettings = analysis.prompt();
        var original = videoSettings.current();
        var update = new com.marketing.agent.analysis.application.VideoAnalysisSettingsService.Update(
                "실제 발언과 화면의 고민을 요약하세요.", original.version(), "gemini-2.5-pro");
        var saved = videoSettings.save(update);
        assertEquals(saved, videoSettings.current());
        assertEquals(contentSettings, analysis.prompt());
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> videoSettings.save(update)).getStatusCode().value());
        var invalid = new com.marketing.agent.analysis.application.VideoAnalysisSettingsService.Update(
                "분석하세요.", saved.version(), "unknown");
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> videoSettings.save(invalid)).getStatusCode().value());
    }

    @Sql(statements = "UPDATE comment_prompt SET model_tier = 'STANDARD' WHERE id = 1")
    @ParameterizedTest
    @EnumSource(ModelTier.class)
    void nextDraftUsesSavedInstructionsAndRejectsAnOlderEdit(ModelTier model) {
        var original = analysis.prompt();
        String instructions = "짧고 담백한 존댓말로 작성하세요.";
        assertEquals(ModelTier.STANDARD, original.modelTier());
        var request = new CommentPromptService.Update(instructions, original.version(), model);
        var updated = analysis.savePrompt(request);
        assertEquals(updated, analysis.prompt());
        var conflict = assertThrows(ResponseStatusException.class, () -> analysis.savePrompt(request));
        assertEquals(409, conflict.getStatusCode().value());

        String body = "가".repeat(40);
        when(ai.generate(eq(model), anyString(), anyString())).thenReturn(body);
        when(ai.generateJson(eq(model), anyString(), anyString())).thenReturn("{}");
        var draft = analysis.generate(new AnalysisController.DraftRequest("영상 제목", "확인한 발언"));
        assertEquals(body, draft.draft());
        verify(ai).generate(eq(model), contains(instructions), contains("확인한 발언"));
        analysis.summarize(new AnalysisController.SummaryRequest("영상 제목", "영상 설명", List.of()));
        verify(ai).generateJson(eq(model), contains("JSON"), contains("영상 설명"));
    }
}
