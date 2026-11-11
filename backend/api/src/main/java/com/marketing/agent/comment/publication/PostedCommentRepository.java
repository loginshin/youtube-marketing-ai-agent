package com.marketing.agent.comment.publication;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostedCommentRepository extends JpaRepository<PostedComment, String> {
    Optional<PostedComment> findByVideoId(String videoId);
}
