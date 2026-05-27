package com.beast.risk.agent;

import com.beast.risk.rag.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class IdentityAgent implements RiskAgent {

    @Override
    public String getAgentName() {
        return "IdentityAgent";
    }

    @Override
    public AgentResult analyze(Transaction txn, List<Document> context) {
        String contextText = context.stream()
                .map(Document::getContent)
                .reduce("", (a, b) -> a + " " + b)
                .toLowerCase();

        double score = contextText.contains("deepfake") || contextText.contains("social_engineering") ? 0.85 : 0.25;
        String reason = (score > 0.7) ? "Deepfake/social engineering indicators present." : "Identity signals clean.";

        return new AgentResult(getAgentName(), score, reason,
                Map.of("deepfake_risk", score > 0.7), 0.79,
                List.of("Identity + behavioral analysis"));
    }
}
