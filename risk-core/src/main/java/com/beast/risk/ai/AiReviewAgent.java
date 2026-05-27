package com.beast.risk.ai;

import com.beast.risk.rag.RagService;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class AiReviewAgent {

    private final LlmFacade llmFacade;
    private final RagService ragService;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    private final AtomicBoolean isRunning = new AtomicBoolean(false); // Prevent overlapping runs

    public AiReviewAgent(LlmFacade llmFacade,
                         RagService ragService,
                         MeterRegistry meterRegistry,
                         Tracer tracer) {
        this.llmFacade = llmFacade;
        this.ragService = ragService;
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void reviewAndOptimize() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("⚠️ AI Review Agent already running - skipping this cycle");
            return;
        }

        long start = System.currentTimeMillis();
        Span span = tracer.spanBuilder("ai.review.optimization.cycle")
                .setAttribute("cycle.start", Instant.now().toString())
                .startSpan();

        try {
            log.info("🚀 AI Review Agent cycle started");

            // 1. Collect current metrics (in real prod: pull from Prometheus)
            String metricsSummary = buildMetricsSummary();

            // 2. Get relevant context from RAG
            String ragContext = ragService.retrieve(
                            "fraud patterns OR latency OR scaling OR cost OR deployment issues", 10)
                    .stream()
                    .map(doc -> doc.getContent())
                    .reduce("", (a, b) -> a + "\n" + b);

            // 3. Get LLM recommendation with tools
            String recommendation = llmFacade.analyzeWithReasoning(metricsSummary, ragContext);

            log.info("✅ AI Review Agent completed cycle");
            log.info("📊 AI Review Recommendation:\n{}", recommendation);

            // Record success metrics
            meterRegistry.counter("ai.review.cycles.success").increment();
            meterRegistry.timer("ai.review.cycle.latency")
                    .record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            log.error("❌ AI Review Agent cycle failed", e);
            meterRegistry.counter("ai.review.cycles.failure").increment();
            span.recordException(e);
        } finally {
            span.end();
            isRunning.set(false);
        }
    }

    private String buildMetricsSummary() {
        // In real prod: query Prometheus / Micrometer
        return """
            p99_latency=145ms (target<100ms), 
            fpr=0.012 (target<0.008), 
            error_rate=0.003, 
            throughput=2450 tps, 
            cost_per_txn=$0.00042
            """;
    }

    // Manual trigger for demos / testing
    public String triggerManualReview() {
        reviewAndOptimize();
        return "✅ Manual AI Review cycle triggered successfully";
    }
}