package com.marketing.agent.db.repository;

import com.marketing.agent.db.entity.CommentJobEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentJobEventRepository extends JpaRepository<CommentJobEvent, Long> {
    List<CommentJobEvent> findByJob_IdOrderByOccurredAtAscIdAsc(Long jobId);
}
