package com.beast.risk.agent;

import com.beast.risk.rag.Document;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.StructuredTaskScope;

@Slf4j
@Service
public class AgentEnsembleEngine {

    private final List<RiskAgent> agents;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    public AgentEnsembleEngine(List<RiskAgent> agents,
                               MeterRegistry meterRegistry,
                               Tracer tracer) {
        this.agents = agents;
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
    }

    public RiskScore ensemble(Transaction txn, List<Document> context) {
        long start = System.currentTimeMillis();
        String correlationId = txn.getCorrelationId();

        Span span = tracer.spanBuilder("agent.ensemble.execute")
                .setAttribute("correlationId", correlationId)
                .setAttribute("transactionId", txn.getId())
                .startSpan();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            log.info("🚀 Starting ensemble for txn={} | correlationId={}", txn.getId(), correlationId);

            List<StructuredTaskScope.Subtask<AgentResult>> subtasks = new ArrayList<>();

            for (RiskAgent agent : agents) {
                var subtask = scope.fork(() -> {
                    log.debug("Agent {} starting analysis", agent.getAgentName());
                    return agent.analyze(txn, context);
                });
                subtasks.add(subtask);
            }

            scope.join();
            scope.throwIfFailed();

            List<AgentResult> results = subtasks.stream().map(StructuredTaskScope.Subtask::get).toList();

            RiskScore finalScore = combineResults(results);

            // Prometheus Metrics
            meterRegistry.timer("agent.ensemble.latency")
                    .record(Duration.ofMillis(System.currentTimeMillis() - start));

            meterRegistry.counter("agent.ensemble.executions").increment();
            meterRegistry.counter("agent.ensemble.failures").increment(0); // just declare

            // Fixed Gauge
            meterRegistry.gauge("agent.ensemble.agents.count", agents.size());

            log.info("✅ Ensemble completed for txn={} | finalScore={:.3f} | agents={}",
                    txn.getId(), finalScore.getScore(), results.size());

            return finalScore;

        } catch (Exception e) {
            log.error("❌ Ensemble failed for txn={} | correlationId={}", txn.getId(), correlationId, e);
            meterRegistry.counter("agent.ensemble.failures").increment();
            span.recordException(e);
            return fallbackToRules(txn);
        } finally {
            span.end();
        }
    }

    private RiskScore combineResults(List<AgentResult> results) {
        double weightedScore = 0.0;
        Map<String, Object> explainability = new LinkedHashMap<>();

        for (AgentResult r : results) {
            double weight = getAgentWeight(r.getAgentName());
            weightedScore += r.getScore() * weight;

            explainability.put(r.getAgentName(), Map.of(
                    "score", r.getScore(),
                    "reason", r.getReason(),
                    "confidence", r.getConfidence()
            ));
        }

        return new RiskScore(weightedScore, explainability);
    }

    private double getAgentWeight(String agentName) {
        return switch (agentName) {
            case "FraudAgent" -> 0.45;
            case "VelocityAgent" -> 0.30;
            case "IdentityAgent" -> 0.25;
            default -> 0.33;
        };
    }

    private RiskScore fallbackToRules(Transaction txn) {
        log.warn("⚠️ Falling back to rules for txn={}", txn.getId());
        return new RiskScore(0.5, Map.of("fallback", "rules-only", "reason", "Ensemble failed - using conservative rules"));
    }
}