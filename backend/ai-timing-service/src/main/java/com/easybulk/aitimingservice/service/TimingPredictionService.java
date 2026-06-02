package com.easybulk.aitimingservice.service;

import com.easybulk.aitimingservice.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimingPredictionService {

    private final PythonBridgeService pythonBridge;
    private final FeatureEngineeringService featureEngineering;
    private final ModelCacheService modelCache;
    private final ObjectMapper objectMapper;

    public TimingResponse predictOptimalTiming(com.easybulk.aitiming.dto.TimingRequest request) {
        log.info("Predicting optimal timing for campaign: {}, type: {}",
                request.getCampaignType(), request.getNlpType());

        try {
            // 1. Générer la clé de cache
            String cacheKey = modelCache.generateCacheKey(
                    request.getCampaignType(),
                    request.getOperateur(),
                    request.getNlpType(),
                    request.getNlpDomain(),
                    request.getMessage()
            );

            // 2. Vérifier le cache
            Optional<TimingResponse> cachedResponse = modelCache.getCachedPrediction(cacheKey);
            if (cachedResponse.isPresent()) {
                log.info("✅ Returning cached prediction");
                return cachedResponse.get();
            }

            // 3. Feature engineering
            FeatureVector features = FeatureVector.builder()
                    .message(request.getMessage())
                    .messageLength(request.getMessageLength())
                    .nbrPages(request.getNbrPages())
                    .build();

            // 4. Appel au modèle Python
            Map<String, Object> predictionResult = pythonBridge.predict(
                    request.getCampaignType(),
                    request.getOperateur(),
                    request.getNlpType(),
                    features
            );

            // 5. Construction de la réponse
            TimingResponse response = buildResponse(request, predictionResult);
            response.setCacheKey(cacheKey);

            // 6. Mise en cache
            modelCache.cachePrediction(cacheKey, response);

            return response;

        } catch (Exception e) {
            log.error("Error predicting timing: {}", e.getMessage(), e);
            throw new RuntimeException("Prediction failed", e);
        }
    }

    public List<TimingResponse> predictBatch(List<com.easybulk.aitiming.dto.TimingRequest> requests) {
        log.info("Batch prediction for {} campaigns", requests.size());

        return requests.stream()
                .map(this::predictOptimalTiming)
                .toList();
    }

    private TimingResponse buildResponse(com.easybulk.aitiming.dto.TimingRequest request,
                                         Map<String, Object> result) {

        @SuppressWarnings("unchecked")
        Map<String, Object> optimalWindow =
                (Map<String, Object>) result.get("optimal_window");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hourlyCurve =
                (List<Map<String, Object>>) result.get("hourly_curve");

        return TimingResponse.builder()
                .optimalWindow(TimingResponse.OptimalWindow.builder()
                        .startHour((Integer) optimalWindow.get("start_hour"))
                        .endHour((Integer) optimalWindow.get("end_hour"))
                        .label((String) optimalWindow.get("label"))
                        .build())
                .bestHour((Integer) result.get("best_hour"))
                .predictionScore((Double) result.get("hybrid_score_peak"))
                .method((String) result.get("method"))
                .hourlyScores(buildHourlyScores(hourlyCurve))
                .metadata(Map.of(
                        "nlp_type", request.getNlpType(),
                        "campaign_type", request.getCampaignType(),
                        "operateur", request.getOperateur(),
                        "model_version", "v13",
                        "alpha", result.get("alpha"),
                        "threshold_pct", result.get("threshold_pct")
                ))
                .build();
    }

    private List<TimingResponse.HourlyScore> buildHourlyScores(
            List<Map<String, Object>> hourlyCurve) {

        return hourlyCurve.stream()
                .map(hourData -> TimingResponse.HourlyScore.builder()
                        .hour((Integer) hourData.get("hour"))
                        .timeScoreRaw((Double) hourData.get("time_score_raw"))
                        .timeScoreNorm((Double) hourData.get("time_score_norm"))
                        .hybridScore((Double) hourData.get("hybrid_score"))
                        .isInWindow((Boolean) hourData.getOrDefault("in_window", false))
                        .build())
                .toList();
    }
}