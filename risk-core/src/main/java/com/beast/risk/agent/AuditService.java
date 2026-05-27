package com.beast.risk.agent;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    public void logDecision(String correlationId, RiskScore score) {
        System.out.println("[AUDIT] Txn=" + correlationId
                + " | Score=" + score.getScore()
                + " | Explainability=" + score.getExplainability());
        // TODO: Later → push to Kafka audit topic
    }
}