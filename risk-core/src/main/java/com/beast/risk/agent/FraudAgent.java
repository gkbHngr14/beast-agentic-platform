package com.beast.risk.agent;

import com.beast.risk.rag.Document;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FraudAgent implements RiskAgent {

    @Override
    public String getAgentName() {
        return "FraudAgent";
    }

    @Override
    public AgentResult analyze(Transaction txn, List<Document> context) {
        double score = 0.0;
        String reason = "No known fraud patterns detected.";
        double confidence = 0.65;
        Map<String, Object> explain = new HashMap<>();

        String contextText = context.stream()
                .map(Document::getContent)
                .reduce("", (a, b) -> a + " " + b)
                .toLowerCase();

        // Pattern 1: Romance Scam + Mule
        if (txn.getAmount() < 50 && (contextText.contains("urgent") || contextText.contains("emergency"))) {
            score = 0.78;
            reason = "Small test transfer followed by urgent language — classic romance scam to mule account.";
            confidence = 0.88;
            explain.put("pattern", "ROMANCE_MULE");
            explain.put("rule_suggestion", "R1: Block transfers >$1000 after test txns < $50 within 24h");
        }

        // Pattern 2: Synthetic Identity + Device Sharing
        if (contextText.contains("multiple_accounts") || contextText.contains("device_shared")) {
            score = 0.72;
            reason = "Device fingerprint shared across suspicious synthetic identities.";
            confidence = 0.81;
            explain.put("pattern", "SYNTHETIC_DEVICE");
            explain.put("rule_suggestion", "R2: Flag accounts sharing device + behavioral entropy");
        }

        return new AgentResult(getAgentName(), score, reason, explain, confidence,
                List.of("Analyzed amount + contextual fraud signals"));
    }
}