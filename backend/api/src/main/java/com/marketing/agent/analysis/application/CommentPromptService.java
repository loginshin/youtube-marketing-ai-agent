package com.marketing.agent.analysis.application;

import com.marketing.agent.db.repository.CommentPromptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentPromptService {
    private final CommentPromptRepository prompts;

    public CommentPromptService(CommentPromptRepository prompts) {
        this.prompts = prompts;
    }

    @Transactional(readOnly = true)
    public Settings current() {
        var prompt = prompts.getById(1);
        return new Settings(prompt.getInstructions(), prompt.getVersion());
    }

    @Transactional
    public Settings save(Settings request) {
        String text = request.instructions() == null ? "" : request.instructions().strip();
        if (text.isBlank() || text.length() > 4000 || request.version() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (prompts.updateIfCurrent(text, request.version()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new Settings(text, request.version() + 1);
    }

    public record Settings(String instructions, Long version) {}
}
