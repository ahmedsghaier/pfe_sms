package com.easybulk.smsgatewayservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "routes")
public class Route {

    @Id
    private String id;

    private String groupId; // "ALL" pour global

    private String alphaHeader;

    private String destinationPattern; // Regex pour le numéro de destination

    private String connectorId;

    private int priority; // 1-199 (plus petit = plus prioritaire)

    private boolean active;

    private boolean isDefault; // La route "Default" (priorité 200)

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}