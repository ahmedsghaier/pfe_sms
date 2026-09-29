package com.easybulk.campaignservice.controller;

import com.easybulk.campaignservice.dto.*;
import com.easybulk.campaignservice.repository.SmsDecisionLogRepository;
import com.easybulk.campaignservice.service.BanditService;
import com.easybulk.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FeedbackController {

    private final SmsDecisionLogRepository decisionLogRepo;
    private final BanditService banditService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> receiveFeedback(
            @Valid @RequestBody FeedbackRequest req) {

        return decisionLogRepo.findBySmsId(req.getSmsId())
                .map(decision -> {
                    decision.setEngaged(req.getEngaged());
                    decision.setEngagedAt(LocalDateTime.now());
                    decision.setFeedbackSource(req.getSource());
                    decisionLogRepo.save(decision);

                    // Point critique : mise à jour du bandit
                    banditService.update(
                            decision.getContextKey(),
                            decision.getRecommendedHour(),
                            req.getEngaged()
                    );

                    return ResponseEntity.ok(ApiResponse.success(
                            "Feedback enregistré",
                            FeedbackResponse.builder()
                                    .smsId(req.getSmsId())
                                    .status("updated")
                                    .engaged(req.getEngaged())
                                    .build()
                    ));
                })
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(
                        "smsId inconnu",
                        FeedbackResponse.builder()
                                .smsId(req.getSmsId()).status("not_found").build()
                )));
    }

    // URL de tracking dans le SMS
    @GetMapping("/click/{smsId}")
    public ResponseEntity<Void> trackClick(
            @PathVariable String smsId,
            @RequestParam(name = "r", required = false) String redirect) {

        decisionLogRepo.findBySmsId(smsId).ifPresent(d -> {
            if (!Boolean.TRUE.equals(d.getEngaged())) {
                d.setEngaged(true);
                d.setEngagedAt(LocalDateTime.now());
                d.setFeedbackSource("click_tracking");
                decisionLogRepo.save(d);
                banditService.update(d.getContextKey(),
                        d.getRecommendedHour(), true);
            }
        });

        if (redirect != null)
            return ResponseEntity.status(302)
                    .header("Location", redirect).build();
        return ResponseEntity.ok().build();
    }
}