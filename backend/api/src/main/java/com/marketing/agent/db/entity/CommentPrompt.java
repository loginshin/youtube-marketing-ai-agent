package com.marketing.agent.db.entity;

import com.marketing.agent.ai.ModelTier;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "comment_prompt")
public class CommentPrompt {
    @Id
    private Integer id;

    @Column(nullable = false, length = 4000)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "model_tier", nullable = false, length = 16)
    private ModelTier modelTier;

    public ModelTier getModelTier() { return modelTier; }

    @Version
    private long version;

    public String getInstructions() { return instructions; }
    public long getVersion() { return version; }
}
