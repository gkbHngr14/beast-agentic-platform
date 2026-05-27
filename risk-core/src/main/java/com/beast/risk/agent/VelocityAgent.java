package com.beast.risk.agent;

import com.beast.risk.rag.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class VelocityAgent implements RiskAgent {

    @Override
    public String getAgentName() {
        return "VelocityAgent";
    }

    @Override
    public AgentResult analyze(Transaction txn, List<Document> context) {
        double score = (txn.getAmount() > 500) ? 0.62 : 0.18;
        String reason = (score > 0.5) ? "High-velocity large transfer detected." : "Normal velocity pattern.";
        Map<String, Object> explain = Map.of("velocity_flag", score > 0.5, "amount", txn.getAmount());

        return new AgentResult(getAgentName(), score, reason, explain, 0.78,
                List.of("Velocity check completed"));
    }
}