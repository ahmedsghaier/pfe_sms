package com.easybulk.aitimingservice.controller;

import com.easybulk.aitiming.dto.TimingRequest;
import com.easybulk.aitimingservice.dto.TimingResponse;
import com.easybulk.aitimingservice.service.TimingPredictionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/timing")
@RequiredArgsConstructor
public class TimingPredictionController {

    private final TimingPredictionService predictionService;

    @PostMapping("/predict")
    public ResponseEntity<TimingResponse> predictTiming(
            @Valid @RequestBody TimingRequest request) {

        log.info("Received timing prediction request for campaign type: {}",
                request.getCampaignType());

        TimingResponse response = predictionService.predictOptimalTiming(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/predict/batch")
    public ResponseEntity<List<TimingResponse>> predictBatch(
            @Valid @RequestBody List<TimingRequest> requests) {

        log.info("Received batch prediction request for {} campaigns",
                requests.size());

        List<TimingResponse> responses = predictionService.predictBatch(requests);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Timing Service is running");
    }
}