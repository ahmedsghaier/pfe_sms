package com.easybulk.campaignservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EngagementPredictionRequest {

    @NotBlank(message = "Le message est requis")
    @Size(min = 10, max = 1000, message = "Le message doit contenir entre 10 et 1000 caractères")
    private String messageTemplate;

    private String startDate; // Optionnel, format ISO
    private String endDate;
    private String sendingWindow;
}