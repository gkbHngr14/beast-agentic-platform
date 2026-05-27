package com.beast.risk.runner;

import com.beast.risk.agent.Transaction;
import com.beast.risk.service.CheckoutEvent;
import com.beast.risk.service.RiskScoringService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TestDataRunner implements CommandLineRunner {

    private final RiskScoringService riskScoringService;

    public TestDataRunner(RiskScoringService riskScoringService) {
        this.riskScoringService = riskScoringService;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n🚀 Running demo transactions...\n");

        for (int i = 1; i <= 3; i++) {
            Transaction txn = new Transaction();
            txn.setId(UUID.randomUUID().toString());
            txn.setUserId("user" + i);
            txn.setMerchantId("merc" + (100 + i));
            txn.setAmount(50.0 + (i * 25));
            txn.setDeviceId("dev" + i);
            txn.setCorrelationId(UUID.randomUUID().toString());

            CheckoutEvent event = new CheckoutEvent(txn, "CHECKOUT");

            System.out.println("→ Sending test transaction #" + i);
            riskScoringService.processCheckout(txn.toString());
        }

        System.out.println("\n✅ Demo transactions completed. Check logs for ensemble scores.\n");
    }
}