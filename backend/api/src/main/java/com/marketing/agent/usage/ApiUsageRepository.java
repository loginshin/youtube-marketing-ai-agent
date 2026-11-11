package com.marketing.agent.usage;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApiUsageRepository extends JpaRepository<ApiUsageEvent, Long> {
    record Summary(String provider, String operation, long calls, long measuredCalls,
                   long inputTokens, long outputTokens) {}

    @Query("""
            select new com.marketing.agent.usage.ApiUsageRepository$Summary(
                u.provider, u.operation, count(u), count(u.inputTokens),
                coalesce(sum(u.inputTokens), 0L), coalesce(sum(u.outputTokens), 0L))
            from ApiUsageEvent u where u.occurredAt >= :start
            group by u.provider, u.operation order by u.provider, count(u) desc, u.operation
            """)
    List<Summary> summarize(Instant start);
}
