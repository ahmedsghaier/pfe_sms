package com.easybulk.engagementpredictionservice.controller;

import com.easybulk.engagementpredictionservice.dto.CampaignPredictionRequest;
import com.easybulk.engagementpredictionservice.dto.CampaignPredictionResponse;
import com.easybulk.engagementpredictionservice.service.EngagementPredictionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/engagement")
@CrossOrigin(origins = "*")
public class EngagementController {

    private final EngagementPredictionService predictionService;

    public EngagementController(EngagementPredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/predict")
    public ResponseEntity<CampaignPredictionResponse> predict(
            @Valid @RequestBody CampaignPredictionRequest request) {
        CampaignPredictionResponse response = predictionService.predictEngagement(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("✅ Engagement Prediction Service is running");
    }
}
