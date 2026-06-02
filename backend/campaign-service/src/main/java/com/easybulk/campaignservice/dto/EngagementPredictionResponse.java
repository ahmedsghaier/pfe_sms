package com.easybulk.campaignservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementPredictionResponse {

    @JsonProperty("nlp_type")
    private String nlpType;

    @JsonProperty("predicted_engagement_rate")
    private Double predictedEngagementRate;

    @JsonProperty("best_hour")
    private Integer bestHour;

    @JsonProperty("best_window")
    private String bestWindow;

    @JsonProperty("chosen_hour_score")
    private Double chosenHourScore;

    private String recommendation;
    private List<WindowScore> topWindows;

    // ── Sous-objet fenêtre optimale ──────────────────────────────────────
    @Data
    @Builder
    public static class WindowScore {
        private String windowKey;    // "BUSINESS_HOURS" | "EVENING" | "ALL_DAY"
        private String windowLabel;  // "Jours ouvrables (08h–18h)"
        private double rate;         // taux d'engagement en %
        private int    peakHour;     // heure de pic dans cette fenêtre
        private String icon;         // emoji représentatif
    }
}