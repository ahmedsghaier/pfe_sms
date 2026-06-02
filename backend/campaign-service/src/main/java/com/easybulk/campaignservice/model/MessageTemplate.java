package com.easybulk.campaignservice.model;

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
@Document(collection = "message_templates")
public class MessageTemplate {

    @Id
    private String id;

    private String userId;

    private String organizationId;

    private String name;

    private String content;

    private TemplateType type;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum TemplateType {
        CLASSIC,           // Variables @nom, @prénom, @email, @téléphone
        TRANSACTIONAL      // Variables {{variable}}
    }
}