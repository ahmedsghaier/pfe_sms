package com.easybulk.aitiming.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TimingRequest {

    @NotNull
    private String campaignType;  // CLASSIC, TRANSACTIONAL

    @NotNull
    private String operateur;  // Orange, Ooredoo, Telecom

    @NotNull
    private String nlpType;  // OTP, Transaction, Promotion, etc.

    private String nlpDomain;  // Banque, Marketing, etc.

    @NotBlank
    private String message;

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
