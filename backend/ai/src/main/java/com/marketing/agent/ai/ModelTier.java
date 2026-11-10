package com.marketing.agent.ai;

public enum ModelTier {
    PREMIUM("고품질", "gpt-5.4"),
    STANDARD("보통", "gpt-5.4-mini"),
    ECONOMY("가성비", "gpt-5.4-nano");

    private final String label;
    private final String modelId;

    ModelTier(String label, String modelId) {
        this.label = label;
        this.modelId = modelId;
    }

    public String label() { return label; }
    public String modelId() { return modelId; }
}
