package com.easybulk.campaignservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("libelle")
    private String name;

    @JsonProperty("message")
    private String content;

    private TemplateType type;

    @JsonProperty("status")
    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum TemplateType {
        CLASSIC,
        TRANSACTIONAL
    }
}