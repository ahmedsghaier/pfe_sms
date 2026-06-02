package com.easybulk.campaignservice.dto;


import com.easybulk.campaignservice.model.Campaign;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateCampaignRequest {

    @NotBlank(message = "Campaign name is required")
    private String name;

    @NotNull(message = "Campaign type is required")
    private Campaign.CampaignType type;

    @NotBlank(message = "Group ID is required")
    private String groupId;

    // Étape 1: Message
    private String messageTemplate;
    private String templateId; // Si on utilise un modèle existant

    // Étape 2: Contacts
    private List<String> contactIds;
    private List<String> contactTags;

    // Étape 3: Paramètres
    private String alphaHeader;
    private LocalDateTime scheduledStartDate;
    private LocalDateTime scheduledEndDate;
    private Integer smsValidityHours;
    private Campaign.SendingWindow sendingWindow;

    // Pour campagnes transactionnelles
    private String apiKeyId;
    private Boolean restrictToApiKey;
}