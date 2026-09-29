package com.easybulk.campaignservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackResponse {

    private String smsId;

    /**
     * updated / not_found
     */
    private String status;

    private Boolean engaged;
}