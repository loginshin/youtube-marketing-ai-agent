package com.marketing.agent.usage;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "api_usage_event")
public class ApiUsageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String provider;
    private String operation;
    private Instant occurredAt;
    private Long inputTokens;
    private Long outputTokens;

    protected ApiUsageEvent() {}

    ApiUsageEvent(String provider, String operation, Long inputTokens, Long outputTokens) {
        this.provider = provider;
        this.operation = operation;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.occurredAt = Instant.now();
    }
}
