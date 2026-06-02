package com.easybulk.aitimingservice.service;


import com.easybulk.aitimingservice.dto.TimingResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai-timing.prediction.cache-enabled", havingValue = "true", matchIfMissing = true)
public class ModelCacheService {

    private static final String CACHE_PREFIX = "ai-timing:prediction:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Génère une clé de cache basée sur les paramètres de la campagne
     */
    public String generateCacheKey(String campaignType,
                                   String operateur,
                                   String nlpType,
                                   String nlpDomain,
                                   String message) {

        String normalizedMessage = message != null
                ? message.toLowerCase().trim()
                : "";

        String messageHash = Integer.toHexString(
                normalizedMessage.hashCode()
        );

        return String.format("%s%s:%s:%s:%s:%s",
                CACHE_PREFIX,
                campaignType,
                operateur,
                nlpType,
                nlpDomain != null ? nlpDomain : "default",
                messageHash
        );
    }

    /**
     * Récupère une prédiction depuis le cache
     */
    public Optional<TimingResponse> getCachedPrediction(String cacheKey) {
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);

            if (cachedJson != null) {
                log.debug("✅ Cache hit for key: {}", cacheKey);
                TimingResponse response = objectMapper.readValue(
                        cachedJson,
                        TimingResponse.class
                );
                return Optional.of(response);
            }

            log.debug("❌ Cache miss for key: {}", cacheKey);
            return Optional.empty();

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached prediction: {}", e.getMessage());
            // Supprimer la clé corrompue
            redisTemplate.delete(cacheKey);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error accessing cache: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Stocke une prédiction dans le cache
     */
    public void cachePrediction(String cacheKey,
                                TimingResponse response,
                                Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, ttl.toMillis(), TimeUnit.MILLISECONDS);

            log.debug("💾 Cached prediction with key: {} (TTL: {})", cacheKey, ttl);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize prediction for caching: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to cache prediction: {}", e.getMessage());
        }
    }

    /**
     * Stocke avec le TTL par défaut
     */
    public void cachePrediction(String cacheKey, TimingResponse response) {
        cachePrediction(cacheKey, response, DEFAULT_TTL);
    }

    /**
     * Invalide une prédiction spécifique
     */
    public void invalidatePrediction(String cacheKey) {
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("🗑️ Invalidated cache for key: {}", cacheKey);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache: {}", e.getMessage());
        }
    }

    /**
     * Invalide toutes les prédictions d'un type de campagne
     */
    public void invalidateByCampaignType(String campaignType) {
        try {
            String pattern = CACHE_PREFIX + campaignType + ":*";
            var keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("🗑️ Invalidated {} predictions for campaign type: {}",
                        deleted, campaignType);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache by campaign type: {}", e.getMessage());
        }
    }

    /**
     * Invalide tout le cache
     */
    public void invalidateAll() {
        try {
            String pattern = CACHE_PREFIX + "*";
            var keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("🗑️ Invalidated all {} cached predictions", deleted);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate all cache: {}", e.getMessage());
        }
    }

    /**
     * Vérifie si une prédiction existe dans le cache
     */
    public boolean exists(String cacheKey) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        } catch (Exception e) {
            log.error("Failed to check cache existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtient les statistiques du cache
     */
    public CacheStats getStats() {
        try {
            String pattern = CACHE_PREFIX + "*";
            var keys = redisTemplate.keys(pattern);

            long totalKeys = keys != null ? keys.size() : 0;

            return CacheStats.builder()
                    .totalEntries(totalKeys)
                    .prefix(CACHE_PREFIX)
                    .defaultTtl(DEFAULT_TTL)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get cache stats: {}", e.getMessage());
            return CacheStats.builder()
                    .totalEntries(0)
                    .prefix(CACHE_PREFIX)
                    .defaultTtl(DEFAULT_TTL)
                    .build();
        }
    }

    /**
     * Classe pour les statistiques du cache
     */
    @lombok.Data
    @lombok.Builder
    public static class CacheStats {
        private long totalEntries;
        private String prefix;
        private Duration defaultTtl;
    }
}
