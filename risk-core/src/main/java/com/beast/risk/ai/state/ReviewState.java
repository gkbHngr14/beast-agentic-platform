package com.beast.risk.ai.state;

import lombok.Data;

import java.util.Map;

@Data
public class ReviewState {
    private String metricsSummary;
    private String ragContext;
    private String analysis;
    private String recommendation;
    private String actionType;           // SCALE, TUNE_WEIGHTS, A_B_TEST, etc.
    private boolean requiresHITL;
    private double confidence;
    private Map<String, Object> metadata;
}