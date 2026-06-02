package com.easybulk.campaignservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimingPredictionResponse {

    // Le service AI retourne "bestHour"
    @JsonProperty("bestHour")
    private Integer optimalHour;

    // Le service AI retourne un objet {startHour, endHour, label}
    @JsonProperty("optimalWindow")
    private OptimalWindowDto optimalWindowObj;

    @JsonProperty("predictionScore")
    private Double predictedEngagementRate;

    @JsonProperty("method")
    private String method;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Nested DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OptimalWindowDto {
        private Integer startHour;
        private Integer endHour;
        private String label;
    }

    // ── Helpers pour CampaignService (compatibilité) ──────────

    public String getDetectedType() {
        if (metadata != null && metadata.get("nlp_type") != null) {
            return (String) metadata.get("nlp_type");
        }
        return "Information";
    }

    public String getOptimalWindow() {
        if (optimalWindowObj != null && optimalWindowObj.getLabel() != null) {
            return optimalWindowObj.getLabel();
        }
        if (optimalHour != null) {
            return (optimalHour - 1) + "h-" + (optimalHour + 1) + "h";
        }
        return "9h-11h";
    }

    public Double getPredictedEngagementRate() {
        if (predictedEngagementRate == null) return 65.0;
        // predictionScore est ~1.05, convertir en pourcentage
        if (predictedEngagementRate <= 2.0) {
            return Math.min(95.0, predictedEngagementRate * 75.0 + 20.0);
        }
        return predictedEngagementRate;
    }
}