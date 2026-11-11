package com.marketing.agent.analysis.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class VideoAnalysisSettings {
    @Id
    private Integer id;
    @Column(nullable = false, length = 4000)
    private String instructions;
    @Column(nullable = false, length = 80)
    private String model;
    @Version
    private long version;

    public String getInstructions() { return instructions; }
    public String getModel() { return model; }
    public long getVersion() { return version; }
}
