package com.easybulk.campaignservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class EngagementPredictionResponse {

    // ── Champs existants (inchangés) ─────────────────────────────────────
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

    // ── Champs ajoutés pour Thompson Sampling ────────────────────────────

    @JsonProperty("sms_id")
    private String smsId;                      // UUID retourné au frontend pour feedback

    @JsonProperty("recommended_hour")
    private Integer recommendedHour;           // heure finale choisie par le bandit TS
    // (peut différer de bestHour du modèle Python)

    @JsonProperty("selection_method")
    private String selectionMethod;            // "ts_ml_hybrid" | "explore" | "ml_only"

    @JsonProperty("hourly_scores")
    private Map<Integer, Double> hourlyScores; // {7: 0.31, 8: 0.72, 9: 0.81, ...}
    // scores ML par heure, utilisés par BanditService

    // ── Sous-objet fenêtre optimale (inchangé) ───────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WindowScore {
        private String windowKey;    // "BUSINESS_HOURS" | "EVENING" | "ALL_DAY"
        private String windowLabel;  // "Jours ouvrables (08h–18h)"
        private double rate;         // taux d'engagement en %
        private int    peakHour;     // heure de pic dans cette fenêtre
        private String icon;         // emoji représentatif
    }
}