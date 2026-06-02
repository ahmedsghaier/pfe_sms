package com.easybulk.campaignservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateCampaignRequest {

    @NotBlank(message = "Test phone number is required")
    private String testPhoneNumber;

    private boolean approved;

    private String rejectionReason;
}