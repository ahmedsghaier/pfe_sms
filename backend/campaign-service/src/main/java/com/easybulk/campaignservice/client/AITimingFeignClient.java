package com.easybulk.campaignservice.client;

import com.easybulk.campaignservice.dto.TimingPredictionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "ai-timing-service",
        url = "${ai.timing.service.url:http://localhost:8086}",
        fallback = AITimingFeignClientFallback.class
)
public interface AITimingFeignClient {

    @PostMapping("/api/v1/timing/predict")
    ResponseEntity<String> predictTiming(   // ← String au lieu de TimingPredictionResponse
                                            @RequestBody TimingPredictionRequest request
    );
}