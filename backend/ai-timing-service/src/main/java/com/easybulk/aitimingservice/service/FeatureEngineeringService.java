package com.easybulk.aitimingservice.service;


import com.easybulk.aitimingservice.dto.FeatureVector;
import com.easybulk.aitiming.dto.TimingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FeatureEngineeringService {

    private static final Map<String, Integer> HOUR_RANGES = Map.of(
            "matin_start", 7,
            "matin_end", 11,
            "apres_m_start", 12,
            "apres_m_end", 16,
            "soir_start", 17,
            "soir_end", 22
    );

    public FeatureVector buildFeatures(TimingRequest request) {
        log.debug("Building feature vector for request");

        LocalDateTime scheduled = request.getScheduledDate() != null
                ? request.getScheduledDate()
                : LocalDateTime.now();

        int hour = scheduled.getHour();
        int month = scheduled.getMonthValue();
        int dayOfWeek = scheduled.getDayOfWeek().getValue() - 1; // 0=Lundi
        int dayOfMonth = scheduled.getDayOfMonth();

        return FeatureVector.builder()
                // Temporal features
                .sendHour(hour)
                .sendMonth(month)
                .sendDayOfWeek(dayOfWeek)
                .dayOfMonth(dayOfMonth)

                // Hour range features
                .isMatin(isInRange(hour, "matin") ? 1 : 0)
                .isApresM(isInRange(hour, "apres_m") ? 1 : 0)
                .isSoir(isInRange(hour, "soir") ? 1 : 0)
                .isPeakMorning((hour >= 8 && hour <= 12) ? 1 : 0)
                .isPeakEvening((hour >= 17 && hour <= 22) ? 1 : 0)

                // Periodic features
                .isSalaryPeriod((dayOfMonth <= 5) ? 1 : 0)
                .isRamadan(request.getIsRamadan() != null && request.getIsRamadan() ? 1 : 0)

                // Campaign features
                .campaignTypeEnc(encodeCampaignType(request.getCampaignType()))
                .messageLength(request.getMessageLength())
                .encodingEnc("UCS2".equals(request.getEncoding()) ? 1 : 0)
                .nbrPages(request.getNbrPages() != null ? request.getNbrPages() : 1)

                // SMSC features
                .smscNameEnc(encodeSmscName(request.getSmscName()))
                .operateurFailureRate(request.getOperateurFailureRate() != null
                        ? request.getOperateurFailureRate() : 0.15)
                .smscId(request.getSmscId() != null ? request.getSmscId() : 1)

                // NLP features
                .urgencyScore(calculateUrgencyScore(request))
                .isOtp("OTP".equals(request.getNlpType()) ? 1 : 0)
                .isPromo("Promotion".equals(request.getNlpType()) ? 1 : 0)
                .isFinancial("Transaction".equals(request.getNlpType()) ? 1 : 0)
                .isAlert("Alerte".equals(request.getNlpType()) ? 1 : 0)
                .nlpProbaMax(0.85)

                // MSISDN features
                .msisdnEngagementRate(request.getMsisdnEngagementRate() != null
                        ? request.getMsisdnEngagementRate() : 0.30)
                .msisdnTier(1.0)
                .msisdnColdStart(0)

                // Interaction features
                .sensitiveXMorning(calculateSensitiveXMorning(request, hour))
                .marketingXEvening(calculateMarketingXEvening(request, hour))
                .transactXMorning(calculateTransactXMorning(request, hour))

                // One-hot encoding
                .typeOhe(buildTypeOhe(request.getNlpType()))
                .domainOhe(buildDomainOhe(request.getNlpDomain()))

                // Target encoding (valeurs par défaut)
                .smscNameTargetLoo(0.30)
                .campaignTypeTargetLoo(0.30)
                .nlpTypeTargetLoo(0.30)

                .build();
    }

    private boolean isInRange(int hour, String range) {
        int start = HOUR_RANGES.get(range + "_start");
        int end = HOUR_RANGES.get(range + "_end");
        return hour >= start && hour <= end;
    }

    private int encodeCampaignType(String type) {
        return "TRANSACTIONAL".equals(type) ? 1 : 0;
    }

    private int encodeSmscName(String smsc) {
        if (smsc == null) return 0;
        return switch (smsc.toLowerCase()) {
            case "orange" -> 0;
            case "ooredoo" -> 1;
            case "telecom" -> 2;
            default -> 0;
        };
    }

    private double calculateUrgencyScore(TimingRequest request) {
        String type = request.getNlpType();
        return switch (type) {
            case "OTP" -> 1.0;
            case "Alerte" -> 0.8;
            case "Transaction" -> 0.6;
            default -> 0.2;
        };
    }

    private double calculateSensitiveXMorning(TimingRequest request, int hour) {
        boolean isSensitive = "OTP".equals(request.getNlpType()) ||
                "Alerte".equals(request.getNlpType());
        boolean isMorning = hour >= 8 && hour <= 12;
        return (isSensitive && isMorning) ? 1.0 : 0.0;
    }

    private double calculateMarketingXEvening(TimingRequest request, int hour) {
        boolean isMarketing = "Promotion".equals(request.getNlpType());
        boolean isEvening = hour >= 17 && hour <= 22;
        return (isMarketing && isEvening) ? 1.0 : 0.0;
    }

    private double calculateTransactXMorning(TimingRequest request, int hour) {
        boolean isTransact = "Transaction".equals(request.getNlpType());
        boolean isMorning = hour >= 8 && hour <= 12;
        return (isTransact && isMorning) ? 1.0 : 0.0;
    }

    private Map<String, Integer> buildTypeOhe(String nlpType) {
        String[] types = {"OTP", "Transaction", "Alerte", "Promotion",
                "Livraison", "Rappel", "Information"};
        Map<String, Integer> ohe = new HashMap<>();
        for (String type : types) {
            ohe.put("type_" + type, type.equals(nlpType) ? 1 : 0);
        }
        return ohe;
    }

    private Map<String, Integer> buildDomainOhe(String domain) {
        String[] domains = {"Banque", "Marketing", "Télécom", "Logistique",
                "Administration", "Support"};
        Map<String, Integer> ohe = new HashMap<>();
        for (String d : domains) {
            ohe.put("domain_" + d, d.equals(domain) ? 1 : 0);
        }
        return ohe;
    }
}
