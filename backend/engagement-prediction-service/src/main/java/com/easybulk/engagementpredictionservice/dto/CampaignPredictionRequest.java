package com.easybulk.engagementpredictionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignPredictionRequest {

    @NotBlank(message = "Le message est obligatoire")
    private String message;

    private String type = "CLASSIC"; // CLASSIC ou TRANSACTIONAL

    private String nlpType;

    private String smscName = "Orange";

    private String domain;

    @Positive
    private Integer estimatedContacts = 1000;

    private String campaignId;
}
