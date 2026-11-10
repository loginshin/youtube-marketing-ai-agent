package com.marketing.agent.db.entity;

import jakarta.persistence.Column;
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

    @Version
    private long version;

    public String getInstructions() { return instructions; }
    public long getVersion() { return version; }
}
