package com.beast.risk.controller;

import com.beast.risk.agent.RiskScore;
import com.beast.risk.agent.Transaction;
import com.beast.risk.ai.AiReviewAgent;
import com.beast.risk.service.RiskScoringService;
import com.beast.risk.service.CheckoutEvent;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/risk")
public class RiskController {

    private final AiReviewAgent aiReviewAgent;
    private final RiskScoringService riskScoringService;

    public RiskController(AiReviewAgent aiReviewAgent, RiskScoringService riskScoringService) {
        this.aiReviewAgent = aiReviewAgent;
        this.riskScoringService = riskScoringService;
    }

    @PostMapping("/score")
    public Map<String, Object> scoreTransaction(@RequestBody TransactionRequest request) {
        Transaction txn = new Transaction();
        txn.setId(UUID.randomUUID().toString());
        txn.setUserId(request.userId());
        txn.setMerchantId(request.merchantId());
        txn.setAmount(request.amount());
        txn.setDeviceId(request.deviceId());
        txn.setCorrelationId(UUID.randomUUID().toString());

        CheckoutEvent event = new CheckoutEvent(txn, "CHECKOUT");

        // Trigger scoring
        riskScoringService.processCheckout(request.toString());

        return Map.of(
                "status", "processed",
                "transactionId", txn.getId(),
                "message", "Check logs for ensemble score"
        );
    }

    @PostMapping("/review/trigger")
    public String triggerReview() {
        try {
            log.info("Manual trigger for AI Review Agent");
            aiReviewAgent.reviewAndOptimize();
            return "✅ AI Review Agent triggered successfully. Check console logs for output.";
        } catch (Exception e) {
            log.error("Failed to trigger AI Review Agent", e);
            return "❌ Failed to trigger: " + e.getMessage();
        }
    }
}

// Simple DTO
record TransactionRequest(String userId, String merchantId, double amount, String deviceId) {}