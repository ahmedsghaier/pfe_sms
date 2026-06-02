package com.easybulk.smsgatewayservice.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "connectors")
public class Connector {

    @Id
    private String id;

    private String name;

    private ConnectorType type;

    private boolean active;

    // Configuration spécifique au type
    private NativeConfig nativeConfig;
    private LoadBalancerConfig loadBalancerConfig;
    private FailoverConfig failoverConfig;

    private boolean available;
    private LocalDateTime lastAvailabilityCheck;
    private Long unavailableDurationMinutes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ConnectorType {
        NATIVE,          // Connexion directe opérateur
        LOAD_BALANCER,   // Répartition sur plusieurs Natives
        FAILOVER         // Principal + Secours
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NativeConfig {
        private String operatorName;
        private String apiUrl;
        private String apiKey;
        private String username;
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoadBalancerConfig {
        private List<String> nativeConnectorIds; // Min 2
        private LoadBalancingStrategy strategy;

        public enum LoadBalancingStrategy {
            ROUND_ROBIN,
            WEIGHTED,
            LEAST_BUSY
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailoverConfig {
        private String primaryConnectorId;
        private String secondaryConnectorId;
    }
}