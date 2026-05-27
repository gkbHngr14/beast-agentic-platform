package com.beast.risk.agent;

import java.util.Map;

public class RiskScore {
    private final double score;
    private final Map<String, Object> explainability;

    public RiskScore(double score, Map<String, Object> explainability) {
        this.score = score;
        this.explainability = explainability;
    }

    public double getScore() { return score; }
    public Map<String, Object> getExplainability() { return explainability; }
}