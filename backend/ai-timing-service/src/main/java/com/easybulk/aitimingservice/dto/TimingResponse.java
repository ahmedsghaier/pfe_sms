package com.easybulk.aitimingservice.dto;

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
public class TimingResponse {

    private OptimalWindow optimalWindow;
    private Integer bestHour;
    private Double predictionScore;
    private String method;
    private List<HourlyScore> hourlyScores;
    private Map<String, Object> metadata;
    private String cacheKey;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimalWindow {
        private Integer startHour;
        private Integer endHour;
        private String label;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyScore {
        private Integer hour;
        private Double timeScoreRaw;
        private Double timeScoreNorm;
        private Double hybridScore;
        private Boolean isInWindow;
    }
}