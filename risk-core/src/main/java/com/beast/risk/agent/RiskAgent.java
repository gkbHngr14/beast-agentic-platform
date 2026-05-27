package com.beast.risk.agent;

import com.beast.risk.rag.Document;

import java.util.List;

public interface RiskAgent {
    AgentResult analyze(Transaction txn, List<Document> context);
    default String getAgentName() { return this.getClass().getSimpleName(); }
}