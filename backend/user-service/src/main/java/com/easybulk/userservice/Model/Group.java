package com.easybulk.userservice.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "groups")
public class Group {

    @Id
    private String id;

    private String organizationId;

    private String name;

    private String description;

    private BigDecimal budget;

    private BigDecimal usedBudget;

    private String alphaHeader; // Entête-alpha pour les SMS

    private CampaignType allowedCampaignType;

    private String adminId; // ID de l'admin assigné au groupe

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum CampaignType {
        CLASSIC,           // Campagnes classiques uniquement
        TRANSACTIONAL,     // Campagnes transactionnelles uniquement
        BOTH               // Les deux types
    }

    public BigDecimal getRemainingBudget() {
        return budget.subtract(usedBudget);
    }
}
