package com.easybulk.userservice.Model;

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
@Document(collection = "api_keys")
public class ApiKey {

    @Id
    private String id;

    private String userId;

    private String label;

    private String keyHash; // Hash SHA-256 de la clé

    private String keyPrefix; // 8 premiers caractères pour identification

    private LocalDateTime expiresAt;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;
}