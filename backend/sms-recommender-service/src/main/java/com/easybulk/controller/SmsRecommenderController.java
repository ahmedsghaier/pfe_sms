package com.easybulk.controller;

import com.easybulk.dto.FeedbackRequest;
import com.easybulk.dto.RecommendRequest;
import com.easybulk.dto.RecommendResponse;
import com.easybulk.service.SmsRecommenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sms-recommender")
@RequiredArgsConstructor
@Slf4j
public class SmsRecommenderController {

    private final SmsRecommenderService service;

    @PostMapping("/recommend")
    public ResponseEntity<RecommendResponse> recommend(
            @RequestBody RecommendRequest request) {
        log.info("Recommend request: '{}'", request.getPartialText());
        RecommendResponse response = service.recommend(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/feedback")
    public ResponseEntity<Void> feedback(
            @RequestBody FeedbackRequest request) {
        log.info("Feedback: '{}' → '{}'", request.getPartialText(), request.getSelectedWord());
        service.recordFeedback(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("sms-recommender-service UP");
    }
}