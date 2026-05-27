package com.beast.risk.service;

import com.beast.risk.agent.AgentEnsembleEngine;
import com.beast.risk.agent.RiskScore;
import com.beast.risk.rag.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RiskScoringService {

    private final ObjectMapper objectMapper;
    private final AgentEnsembleEngine ensembleEngine;
    private final RagService ragService;
    private final MeterRegistry meterRegistry;

    public RiskScoringService(AgentEnsembleEngine ensembleEngine,
                              RagService ragService,
                              MeterRegistry meterRegistry,
                              ObjectMapper objectMapper) {
        this.ensembleEngine = ensembleEngine;
        this.ragService = ragService;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "checkout-events",
            groupId = "risk-scoring-group",
            concurrency = "${scoring.consumer.concurrency:8}")
    public void processCheckout(String rawMessage) {
        long start = System.currentTimeMillis();
        try {
            log.info("✅ FULL MESSAGE RECEIVED: {}", rawMessage);

            // Convert JSON string to Java object
            CheckoutEvent event = objectMapper.readValue(rawMessage, CheckoutEvent.class);

            log.info("✅ Successfully converted to CheckoutEvent for user: {}",
                    event.getTransaction().getUserId());

            // Run the full agent ensemble
            var similarity = ragService.retrieve("Risk context for " + event.getTransaction().getUserId(), 5);
            RiskScore score = ensembleEngine.ensemble(event.getTransaction(), similarity);

            publishRiskDecision(score);

            meterRegistry.timer("risk.ensemble.latency")
                    .record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
            meterRegistry.counter("risk.decisions.processed").increment();

        } catch (Exception e) {
            log.error("Failed to process message: {}", rawMessage, e);
            meterRegistry.counter("risk.decisions.errors").increment();
        }
    }
    /*
    public void processCheckout(CheckoutEvent event) {
        long start = System.currentTimeMillis();
        try {
            log.info("✅ Processing CheckoutEvent for user: {}", event.getTransaction().getUserId());

            var similarity = ragService.retrieve("Risk context for " + event.getTransaction().getUserId(), 5);

            RiskScore score = ensembleEngine.ensemble(event.getTransaction(), similarity);

            publishRiskDecision(score);

            meterRegistry.timer("risk.ensemble.latency")
                    .record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
            meterRegistry.counter("risk.decisions.processed").increment();

        } catch (Exception e) {
            meterRegistry.counter("risk.decisions.errors").increment();
            log.error("Failed to process checkout event", e);
        }
    }

     */

    private void publishRiskDecision(RiskScore score) {
        log.info("🎯 Final Risk Score: {}", score.getScore());
    }
}