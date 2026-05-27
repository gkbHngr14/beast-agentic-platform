package com.beast.risk.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @JsonProperty("id")
    private String id;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("merchantId")
    private String merchantId;

    @JsonProperty("amount")
    private double amount;

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("correlationId")
    private String correlationId;
}