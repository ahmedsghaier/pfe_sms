package com.easybulk.campaignservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimingPredictionRequest {

    @NotNull
    private String campaignType;  // CLASSIC, TRANSACTIONAL

    @NotNull
    private String operateur;  // Orange, Ooredoo, Telecom

    @NotNull
    private String nlpType;  // OTP, Transaction, Promotion, etc.

    private String nlpDomain;  // Banque, Marketing, etc.

    @NotBlank
    private String message;  // ⬅️ Changé de messageContent à message

    @Min(1)
    private Integer messageLength;

    private String encoding;  // UTF-8, UCS2

    @Min(1)
    private Integer nbrPages;

    private String smscName;

    private Double operateurFailureRate;

    private Integer smscId;

    private LocalDateTime scheduledDate;

    private Integer scheduleHourStart;

    private Integer scheduleHourEnd;

    // MSISDN features
    private String msisdn;

    private Double msisdnEngagementRate;

    private Integer msisdnSmsCount;

    // Optional features
    private Integer groupeId;

    private Boolean isRamadan;

    private Integer smsLast24h;
}