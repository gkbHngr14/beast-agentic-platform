package com.beast.risk.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.beast.risk.agent.Transaction;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutEvent {
    @JsonProperty("transaction")
    private Transaction transaction;

    @JsonProperty("eventType")
    private String eventType;
    /*
    public CheckoutEvent() {}
    public CheckoutEvent(Transaction transaction, String eventType) {
        this.transaction = transaction;
        this.eventType = eventType;
    }

     */

    /*
    // Helper for testing
    public CheckoutEvent(Transaction transaction) {
        this.transaction = transaction;
        this.eventType = "CHECKOUT";
    }

     */

    public String getContextQuery() {
        return "Risk analysis for user " + transaction.getUserId()
                + " merchant " + transaction.getMerchantId();
    }
}