package com.easybulk.campaignservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackRequest {

    /**
     * UUID reçu depuis /predict-engagement
     */
    @NotBlank(message = "smsId requis")
    private String smsId;

    /**
     * true = cliqué / répondu
     * false = pas d'engagement
     */
    @NotNull(message = "engaged requis")
    private Boolean engaged;

    /**
     * Source du feedback :
     * click / manual / delivery
     */
    private String source;
}