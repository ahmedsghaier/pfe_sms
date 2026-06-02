package com.easybulk.campaignservice.controller;


import com.easybulk.campaignservice.dto.CampaignStatsResponse;
import com.easybulk.campaignservice.dto.CreateCampaignRequest;
import com.easybulk.campaignservice.dto.ValidateCampaignRequest;
import com.easybulk.campaignservice.dto.EngagementPredictionRequest;
import com.easybulk.campaignservice.dto.EngagementPredictionResponse;
import com.easybulk.campaignservice.model.Campaign;
import com.easybulk.campaignservice.model.SmsLog;
import com.easybulk.campaignservice.service.CampaignService;
import com.easybulk.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    public ResponseEntity<ApiResponse<Campaign>> createCampaign(
            @RequestHeader("X-Organization-Id") String organizationId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateCampaignRequest request) {

        Campaign campaign = campaignService.createCampaign(organizationId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Campaign created successfully", campaign));
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<Campaign>> updateCampaign(
            @PathVariable String campaignId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateCampaignRequest request) {

        Campaign campaign = campaignService.updateCampaign(campaignId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Campaign updated successfully", campaign));
    }

    @PostMapping("/{campaignId}/submit")
    public ResponseEntity<ApiResponse<Campaign>> submitForValidation(
            @PathVariable String campaignId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {

        Campaign campaign = campaignService.submitForValidation(campaignId, userId, userRole);
        return ResponseEntity.ok(ApiResponse.success("Campaign submitted", campaign));
    }

    @PostMapping("/{campaignId}/validate")
    public ResponseEntity<ApiResponse<Campaign>> validateCampaign(
            @PathVariable String campaignId,
            @RequestHeader("X-User-Id") String validatorId,
            @Valid @RequestBody ValidateCampaignRequest request) {

        Campaign campaign = campaignService.validateCampaign(campaignId, validatorId, request);
        return ResponseEntity.ok(ApiResponse.success("Campaign validation completed", campaign));
    }

    @PostMapping("/{campaignId}/test")
    public ResponseEntity<ApiResponse<Void>> sendTestSms(
            @PathVariable String campaignId,
            @RequestParam String phoneNumber) {

        campaignService.sendTestSms(campaignId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Test SMS sent successfully", null));
    }

    @PostMapping("/{campaignId}/stop")
    public ResponseEntity<ApiResponse<Campaign>> stopCampaign(
            @PathVariable String campaignId,
            @RequestHeader("X-User-Id") String userId) {

        Campaign campaign = campaignService.stopCampaign(campaignId, userId);
        return ResponseEntity.ok(ApiResponse.success("Campaign stopped", campaign));
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<ApiResponse<Page<Campaign>>> getCampaignsByOrganization(
            @PathVariable String organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Campaign> campaigns = campaignService.getCampaignsByOrganization(
                organizationId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<Page<Campaign>>> getCampaignsByGroup(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Campaign> campaigns = campaignService.getCampaignsByGroup(
                groupId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @GetMapping("/my-campaigns")
    public ResponseEntity<ApiResponse<Page<Campaign>>> getMyCampaigns(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Campaign> campaigns = campaignService.getCampaignsByOwner(
                userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<Campaign>> getCampaign(@PathVariable String campaignId) {
        Campaign campaign = campaignService.getCampaignById(campaignId);
        return ResponseEntity.ok(ApiResponse.success(campaign));
    }

    @GetMapping("/{campaignId}/stats")
    public ResponseEntity<ApiResponse<CampaignStatsResponse>> getCampaignStats(
            @PathVariable String campaignId) {

        CampaignStatsResponse stats = campaignService.getCampaignStats(campaignId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/{campaignId}/logs")
    public ResponseEntity<ApiResponse<Page<SmsLog>>> getCampaignLogs(
            @PathVariable String campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<SmsLog> logs = campaignService.getCampaignLogs(
                campaignId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
    @PostMapping("/predict-engagement")
    public ResponseEntity<ApiResponse<EngagementPredictionResponse>> predictEngagement(
            @RequestHeader(value = "X-Organization-Id", required = false) String organizationId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody EngagementPredictionRequest request) {

        log.info("Prediction request for message length: {}",
                request.getMessageTemplate().length());

        EngagementPredictionResponse prediction =
                campaignService.predictEngagement(request);

        return ResponseEntity.ok(
                ApiResponse.success("Prédiction effectuée avec succès", prediction)
        );
    }
}