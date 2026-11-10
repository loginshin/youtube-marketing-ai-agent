package com.marketing.agent.db.repository;

import com.marketing.agent.db.entity.CommentJobEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentJobEventRepository extends JpaRepository<CommentJobEvent, Long> {
}
