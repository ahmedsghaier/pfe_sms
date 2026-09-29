package com.easybulk.campaignservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sms_decision_logs")
public class SmsDecisionLog {

    @Id
    private String id;

    /**
     * Liaison avec le feedback (retourné au site web)
     */
    @Indexed(unique = true)
    private String smsId;

    /**
     * Contexte de la décision
     * (même structure que AITimingFeignClient)
     */
    private String organizationId;
    private String campaignId;
    private String nlpType;      // vient de TimingPredictionResponse
    private String operateur;    // vient de EngagementPredictionRequest
    private String campaignType; // CLASSIC / TRANSACTIONAL
    private Integer dayOfWeek;   // 0=lundi ... 6=dimanche

    /**
     * Clé bandit
     * Exemple : CLASSIC_Orange_OTP_2
     */
    private String contextKey;

    /**
     * Décision prise
     */
    private Integer recommendedHour;
    private Double mlScore;
    private String selectionMethod; // ts_ml_hybrid / explore / ml_only
    private LocalDateTime decidedAt;

    /**
     * Feedback
     * null = en attente
     * true/false = reçu
     */
    private Boolean engaged;
    private LocalDateTime engagedAt;
    private String feedbackSource; // click / manual / delivery

    /**
     * TTL MongoDB : suppression automatique après 90 jours
     */
    @Indexed(expireAfterSeconds = 7776000)
    private LocalDateTime expiresAt;
}
