package com.beast.risk.agent;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class AgentResult {
    private final String agentName;
    private final double score;
    private final String reason;
    private final Map<String, Object> explainability;
    private final double confidence;
    private final List<String> auditEvents;
    private final Instant timestamp;

    public AgentResult(String agentName, double score, String reason, Map<String, Object> explainability,
                       double confidence, List<String> auditEvents) {
        this.agentName = agentName;
        this.score = score;
        this.reason = reason;
        this.explainability = Map.copyOf(explainability);
        this.confidence = confidence;
        this.auditEvents = List.copyOf(auditEvents);
        this.timestamp = Instant.now();
    }

    // getters + toAuditString() method for logging
}