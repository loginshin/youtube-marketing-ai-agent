package com.marketing.agent.analysis.application;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VideoAnalysisSettingsService {
    private static final List<Model> MODELS = List.of(
            new Model("gemini-3.8-flash", "균형형", "gemini-3.8-flash"),
            new Model("gemini-2.5-pro", "고품질", "gemini-2.5-pro"));
    private final VideoAnalysisSettingsRepository repository;

    public VideoAnalysisSettingsService(VideoAnalysisSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Settings current() {
        var settings = repository.getById(1);
        return new Settings(settings.getInstructions(), settings.getVersion(), settings.getModel(), MODELS);
    }

    @Transactional
    public Settings save(Update update) {
        String text = update.instructions() == null ? "" : update.instructions().strip();
        if (text.isEmpty() || text.length() > 4000 || update.version() == null
                || MODELS.stream().noneMatch(model -> model.tier().equals(update.modelTier()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (repository.updateIfCurrent(text, update.modelTier(), update.version()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new Settings(text, update.version() + 1, update.modelTier(), MODELS);
    }

    public record Model(String tier, String label, String modelId) {}
    public record Settings(String instructions, long version, String modelTier, List<Model> models) {}
    public record Update(String instructions, Long version, String modelTier) {}
}
