package com.easybulk.aitimingservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class FeatureVector {

    // Phase 1 features
    private Integer sendHour;
    private Integer sendMonth;
    private Integer sendDayOfWeek;
    private Integer dayOfMonth;

    // Binary features
    private Integer isMatin;
    private Integer isApresM;
    private Integer isSoir;
    private Integer isPeakMorning;
    private Integer isPeakEvening;
    private Integer isSalaryPeriod;
    private Integer isRamadan;

    // Campaign features
    private Integer campaignTypeEnc;
    private String message;
    private Integer messageLength;
    private Integer encodingEnc;
    private Integer nbrPages;

    // SMSC features
    private Integer smscNameEnc;
    private Double operateurFailureRate;
    private Integer smscId;

    // NLP features
    private Double urgencyScore;
    private Integer isOtp;
    private Integer isPromo;
    private Integer isFinancial;
    private Integer isAlert;
    private Double nlpProbaMax;

    // MSISDN features
    private Double msisdnEngagementRate;
    private Double msisdnTier;
    private Integer msisdnColdStart;

    // Interaction features
    private Double sensitiveXMorning;
    private Double marketingXEvening;
    private Double transactXMorning;

    // One-hot encoded features
    private Map<String, Integer> typeOhe;
    private Map<String, Integer> domainOhe;

    // Target encoding features
    private Double smscNameTargetLoo;
    private Double campaignTypeTargetLoo;
    private Double nlpTypeTargetLoo;
}