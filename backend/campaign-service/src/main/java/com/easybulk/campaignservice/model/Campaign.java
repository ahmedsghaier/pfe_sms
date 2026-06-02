package com.easybulk.campaignservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "campaigns")
public class Campaign {

    @Id
    private String id;

    @Indexed
    private String organizationId;

    @Indexed
    private String groupId;

    private String name;

    private CampaignType type;

    private CampaignStatus status;

    private String ownerId; // Créateur de la campagne

    // Message
    private String messageTemplate;
    private String messageContent; // Après remplacement des variables
    private int messageLength;
    private int numberOfPages; // Nombre de SMS par message

    // Destinataires
    private List<String> contactIds = new ArrayList<>();
    private List<String> contactTags = new ArrayList<>();
    private int totalRecipients;

    // Paramètres
    private String alphaHeader;
    private LocalDateTime scheduledStartDate;
    private LocalDateTime scheduledEndDate;
    private int smsValidityHours;
    private SendingWindow sendingWindow;

    // Budget
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;

    // Statistiques
    private int sentCount;
    private int deliveredCount;
    private int failedCount;
    private int pendingCount;

    // Workflow de validation
    private String validatedBy;
    private LocalDateTime validatedAt;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;

    // Test
    private boolean testCompleted;
    private String testPhoneNumber;

    // API Key (pour campagnes transactionnelles)
    private String apiKeyId;
    private boolean restrictToApiKey;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public enum CampaignType {
        CLASSIC,           // Campagne classique (envoi masse planifié)
        TRANSACTIONAL      // Campagne transactionnelle (API)
    }

    public enum CampaignStatus {
        DRAFT,                    // En cours de création
        PENDING_VALIDATION,       // À valider (Agent)
        BUDGET_EXCEEDED,          // Dépassement budgétaire
        PENDING_PREPARATION,      // En attente de préparation
        SCHEDULED,                // Planifiée
        IN_MODIFICATION,          // En cours de modification
        CANCELLED,                // Annulée
        REJECTED,                 // Refusée
        IN_PROGRESS,              // En cours d'envoi
        ACTIVE,                   // Active (transactionnelle)
        COMPLETED,                // Clôturée
        SYSTEM_ERROR,             // Erreur système
        PARTIALLY_SENT,           // Envoyée partiellement
        STOPPED                   // Arrêtée
    }

    public enum SendingWindow {
        FULL_TIME,        // 24h/24
        BUSINESS_HOURS,   // 8h-18h (jours ouvrables)
        EVENING           // 18h-22h
    }

    public double getDeliveryRate() {
        if (totalRecipients == 0) return 0;
        return (deliveredCount * 100.0) / totalRecipients;
    }

    public double getProgressPercentage() {
        if (totalRecipients == 0) return 0;
        return ((sentCount + failedCount) * 100.0) / totalRecipients;
    }
}
