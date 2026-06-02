package com.easybulk.userservice.dto;


import com.easybulk.userservice.Model.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "Group name is required")
    private String name;

    private String description;

    @NotNull(message = "Budget is required")
    @Positive(message = "Budget must be positive")
    private BigDecimal budget;

    @NotBlank(message = "Alpha header is required")
    private String alphaHeader;

    @NotNull(message = "Campaign type is required")
    private Group.CampaignType allowedCampaignType;

    private String adminId;
}