package com.marketing.agent.db.repository;

import com.marketing.agent.db.entity.CommentPrompt;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface CommentPromptRepository extends Repository<CommentPrompt, Integer> {
    CommentPrompt getById(Integer id);

    @Modifying
    @Query("update CommentPrompt p set p.instructions = :text, p.version = p.version + 1 where p.id = 1 and p.version = :version")
    int updateIfCurrent(String text, long version);
}
