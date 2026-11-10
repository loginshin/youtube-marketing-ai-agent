package com.marketing.agent.db.repository;

import com.marketing.agent.db.entity.CommentJob;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentJobRepository extends JpaRepository<CommentJob, Long> {
    Optional<CommentJob> findByVideoId(String videoId);
    boolean existsByVideoId(String videoId);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant start, Instant end);

    @Query(value = "SELECT id FROM approval_guard WHERE id = 1 FOR UPDATE", nativeQuery = true)
    int lockApprovalGuard();
}
