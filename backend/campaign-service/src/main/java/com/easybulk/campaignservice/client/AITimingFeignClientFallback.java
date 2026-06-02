package com.easybulk.campaignservice.client;

import com.easybulk.campaignservice.dto.TimingPredictionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AITimingFeignClientFallback implements AITimingFeignClient {

    @Override
    public ResponseEntity<String> predictTiming(TimingPredictionRequest request) {
        log.warn("AI Timing Service unavailable — using fallback");
        // JSON minimal pour le fallback
        String json = """
            {
              "bestHour": 10,
              "predictionScore": 1.05,
              "method": "fallback",
              "optimalWindow": {
                "startHour": 9,
                "endHour": 11,
                "label": "9h-11h"
              },
              "metadata": {
                "nlp_type": "Information",
                "campaign_type": "CLASSIC",
                "operateur": "Orange",
                "model_version": "v13"
              }
            }
            """;
        return ResponseEntity.ok(json);
    }
}