package com.easybulk.engagementpredictionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class CampaignPredictionResponse {

    private double predictedEngagementRate;   // entre 0 et 1
    private String bestTimeWindow;            // ex: "08h-11h"
    private String recommendedPeriod;         // matin / soir / après-midi
    private double confidence;
    private List<HourlyScore> hourlyCurve;
    private String recommendationMessage;
    private int estimatedEngaged;
}

@Data
class HourlyScore {
    private int hour;
    private double score;
}
