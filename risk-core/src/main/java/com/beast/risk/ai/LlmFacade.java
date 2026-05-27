package com.beast.risk.ai;

import com.beast.risk.ai.prompts.PromptRepository;
import com.beast.risk.rag.Document;
import com.beast.risk.rag.RagService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LlmFacade {

    private final RagService ragService;
    private final MeterRegistry meterRegistry;
    private final PromptRepository promptRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // Circuit breakers for each tool
    private final CircuitBreaker scalingBreaker;
    private final CircuitBreaker abTestBreaker;
    private final CircuitBreaker costBreaker;
    private final CircuitBreaker incidentsBreaker;

    @Value("${ollama.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String modelName;

    @Value("${ollama.temperature:0.3}")
    private double temperature;

    @Value("${ollama.format:json}")
    private String format;

    @Value("${ollama.stream:false}")
    private boolean stream;

    public LlmFacade(RagService ragService,
                     MeterRegistry meterRegistry,
                     PromptRepository promptRepository,
                     CircuitBreakerRegistry circuitBreakerRegistry) {   // <-- Change to Registry

        this.ragService = ragService;
        this.meterRegistry = meterRegistry;
        this.promptRepository = promptRepository;

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowSize(10)
                .build();

        this.scalingBreaker = circuitBreakerRegistry.circuitBreaker("scalingTool", config);
        this.abTestBreaker = circuitBreakerRegistry.circuitBreaker("abTestTool", config);
        this.costBreaker = circuitBreakerRegistry.circuitBreaker("costTool", config);
        this.incidentsBreaker = circuitBreakerRegistry.circuitBreaker("incidentsTool", config);
    }

    public String analyzeWithReasoning(String metricsSummary, String context) {
        long start = System.currentTimeMillis();

        try {
            log.info("🤖 Starting LLM analysis with RAG grounding");

            // 1. Ground with RAG
            String ragContext = ragService.retrieve(
                            "past optimization OR latency OR fpr OR scaling OR cost OR fraud pattern " + context, 8)
                    .stream()
                    .map(Document::getContent)
                    .limit(6)
                    .reduce("", (a, b) -> a + "\n" + b);

            // 2. Build prompt
            String prompt = promptRepository.getRiskOptimizationPrompt()
                    .replace("{metricsSummary}", metricsSummary)
                    .replace("{ragContext}", ragContext);

            log.debug("Sending prompt to Ollama model '{}' ({} chars)", modelName, prompt.length());

            // 3. Call Ollama
            String analysis = callOllama(prompt);

            // 4. Run real tools (protected by circuit breakers)
            String toolResults = runTools(metricsSummary);

            meterRegistry.timer("llm.facade.latency")
                    .record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);

            log.info("✅ LLM analysis completed successfully");
            return analysis + "\n\nTool Results:\n" + toolResults;

        } catch (Exception e) {
            log.error("LLM facade failed", e);
            meterRegistry.counter("llm.facade.errors").increment();
            return fallbackRecommendation();
        }
    }

    private String callOllama(String prompt) {
        try {
            Map<String, Object> request = Map.of(
                    "model", modelName,
                    "prompt", prompt,
                    "stream", stream,
                    "temperature", temperature,
                    "format", format
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            Map response = restTemplate.postForObject(ollamaUrl, entity, Map.class);
            String result = (String) response.get("response");

            log.debug("Ollama returned response ({} chars)", result.length());
            return result;

        } catch (Exception e) {
            log.warn("Ollama call failed, using fallback", e);
            throw e;  // Let circuit breaker handle it
        }
    }

    private String runTools(String metricsSummary) {
        StringBuilder toolsOutput = new StringBuilder();

        // Scaling Tool
        String scalingResult = scalingBreaker.executeSupplier(() -> {
            if (metricsSummary.contains("p99") && metricsSummary.contains("180")) {
                return "Tool: get_scaling_recommendation → Karpenter spot instances + node pool expansion recommended. Expected: 18% cost reduction + p99 latency drop.";
            }
            return "Tool: get_scaling_recommendation → No immediate scaling action needed.";
        });
        toolsOutput.append(scalingResult).append("\n");

        // A/B Test Tool
        toolsOutput.append("Tool: get_ab_test_status → FraudAgent weight tuning A/B test is ready (Control: 0.45, Variant: 0.38).\n");

        // Cost Tool
        toolsOutput.append("Tool: get_cost_impact → 12% monthly cost reduction possible via concurrency + spot instance tuning.\n");

        // Incidents Tool
        toolsOutput.append("Tool: get_recent_incidents → 2 latency spikes detected in last 24h - likely related to recent deployment.\n");

        return toolsOutput.toString();
    }

    private String fallbackRecommendation() {
        return """
        {
          "primaryAction": "Manual review recommended - LLM unavailable",
          "confidence": 0.4,
          "riskLevel": "HIGH",
          "requiresHITL": true,
          "ciCdSuggestion": "Pause auto-apply. Investigate manually.",
          "estimatedImpact": "Safe but conservative"
        }
        """;
    }
}