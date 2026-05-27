package com.beast.risk.ai.prompts;

import org.springframework.stereotype.Component;

@Component
public class PromptRepository {

    public String getRiskOptimizationPrompt() {
        return """
        You are a Distinguished Fraud Architect with 15+ years building risk systems at PayPal, Stripe, and Early Warning (Zelle).

        Current metrics:
        {metricsSummary}

        Recent fraud signals and patterns from RAG:
        {ragContext}

        Deliver a crisp, production-grade optimization plan.

        Return ONLY valid JSON (no extra text):

        {
          "primaryAction": "One-line executive summary of highest priority action",
          "confidence": 0.XX,
          "riskLevel": "HIGH/MEDIUM/LOW",
          "requiresHITL": true/false,
          "recommendedRules": [
            {"ruleId": "R1", "description": "Very specific, immediately deployable rule (example: 'Block P2P transfers > $1,000 within 24h of any test transaction <$50 with emotional/urgent language')", "expectedImpact": "X% fraud loss reduction"}
          ],
          "llmBasedSolutions": [
            {"name": "Deepfake Urgency + Narrative Detector", "description": "LLM analyzes message urgency, emotional manipulation patterns, and known scam scripts in real-time", "expectedImpact": "Y% improvement"},
            {"name": "Synthetic Identity + Device Graph Cluster Analyzer", "description": "Graph-based clustering of device fingerprints, behavioral entropy, and rapid account linking", "expectedImpact": "Z% improvement"}
          ],
          "ciCdSuggestion": "Specific A/B test plan, canary rollout criteria, and rollback trigger",
          "estimatedImpact": "Overall fraud loss reduction of X%, approval rate impact of Y%"
        }
        """;
    }
}