package com.marketing.agent.analysis;

import com.marketing.agent.ai.OpenAiClient;
import com.marketing.agent.analysis.application.CommentPromptService;
import com.marketing.agent.analysis.presentation.AnalysisController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:comment-prompt-test")
class CommentPromptTests {
    @Autowired AnalysisController analysis;
    @MockitoBean OpenAiClient ai;

    @Test
    void nextDraftUsesSavedInstructionsAndRejectsAnOlderEdit() {
        var original = analysis.prompt();
        String instructions = "짧고 담백한 존댓말로 작성하세요.";
        var updated = analysis.savePrompt(new CommentPromptService.Settings(instructions, original.version()));
        assertEquals(updated, analysis.prompt());
        var conflict = assertThrows(ResponseStatusException.class, () -> analysis.savePrompt(original));
        assertEquals(409, conflict.getStatusCode().value());

        String body = "가".repeat(40);
        when(ai.generate(anyString(), anyString())).thenReturn(body);
        var draft = analysis.generate(new AnalysisController.DraftRequest("영상 제목", "확인한 발언"));
        assertEquals(body, draft.draft());
        verify(ai).generate(contains(instructions), contains("확인한 발언"));
    }
}
