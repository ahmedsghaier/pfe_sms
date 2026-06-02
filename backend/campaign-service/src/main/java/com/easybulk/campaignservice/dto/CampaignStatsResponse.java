package com.easybulk.campaignservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStatsResponse {

    private String campaignId;
    private String campaignName;

    private int totalRecipients;
    private int sentCount;
    private int deliveredCount;
    private int failedCount;
    private int pendingCount;

    private double deliveryRate;
    private double progressPercentage;

    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
}
