package com.marketing.agent.analysis.application;

import com.marketing.agent.ai.ModelTier;
import com.marketing.agent.db.repository.CommentPromptRepository;
import java.util.Arrays;
import java.util.List;
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
        return new Settings(prompt.getInstructions(), prompt.getVersion(), prompt.getModelTier());
    }

    @Transactional
    public Settings save(Update request) {
        String text = request.instructions() == null ? "" : request.instructions().strip();
        if (text.isBlank() || text.length() > 4000 || request.version() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        // Older clients send only the prompt and version.
        ModelTier model = request.modelTier() == null ? current().modelTier() : request.modelTier();
        if (prompts.updateIfCurrent(text, model, request.version()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new Settings(text, request.version() + 1, model);
    }

    public record Update(String instructions, Long version, ModelTier modelTier) {}
    public record ModelOption(ModelTier tier, String label, String modelId) {}
    public record Settings(String instructions, long version, ModelTier modelTier) {
        public List<ModelOption> getModels() {
            return Arrays.stream(ModelTier.values())
                    .map(tier -> new ModelOption(tier, tier.label(), tier.modelId()))
                    .toList();
        }
    }
}
