package com.easybulk.campaignservice.service;

import com.easybulk.campaignservice.repository.SmsDecisionLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class BanditService {

    private final SmsDecisionLogRepository decisionLogRepo;
    private final ObjectMapper objectMapper;

    @Value("${bandit.state.path:./bandit_state.json}")
    private String statePath;

    @Value("${bandit.decay:0.98}")
    private double decay;

    @Value("${bandit.exploration.rate:0.10}")
    private double explorationRate;

    @Value("${bandit.hybrid.weight:0.5}")
    private double hybridWeight;

    /**
     * Heures autorisées (7h → 23h)
     */
    private static final List<Integer> HOURS = List.of(
            7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23
    );

    /**
     * contextKey -> hour -> [alpha, beta]
     */
    private final ConcurrentHashMap<String, Map<Integer, double[]>> contexts =
            new ConcurrentHashMap<>();

    private final Random random = new Random();

    @PostConstruct
    public void init() {
        loadState();
        log.info("BanditService démarré : {} contextes chargés", contexts.size());
    }

    /**
     * Construction de la clé de contexte.
     */
    public String buildContextKey(
            String campaignType,
            String operateur,
            String nlpType,
            int dayOfWeek
    ) {
        return campaignType + "_"
                + operateur + "_"
                + nlpType + "_"
                + dayOfWeek;
    }

    /**
     * Sélection de l'heure optimale.
     */
    public BanditDecision selectHour(
            String contextKey,
            Map<Integer, Double> mlScores
    ) {

        initContext(contextKey);

        if (detectDrift(contextKey)) {
            resetContext(contextKey);
            log.warn("Drift détecté → reset contexte : {}", contextKey);
        }

        if (random.nextDouble() < explorationRate) {

            int hour = HOURS.get(random.nextInt(HOURS.size()));

            return new BanditDecision(
                    hour,
                    "explore",
                    mlScores.getOrDefault(hour, 0.3)
            );
        }

        Map<Integer, Double> finalScores = new HashMap<>();
        Map<Integer, double[]> ctx = contexts.get(contextKey);

        for (Integer hour : HOURS) {

            double[] params = ctx.get(hour);

            double tsScore = sampleBeta(
                    params[0],
                    params[1]
            );

            double mlScore = mlScores.getOrDefault(hour, 0.3);

            finalScores.put(
                    hour,
                    (1 - hybridWeight) * tsScore
                            + hybridWeight * mlScore
            );
        }

        int bestHour = finalScores.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(9);

        return new BanditDecision(
                bestHour,
                "ts_ml_hybrid",
                mlScores.getOrDefault(bestHour, 0.3)
        );
    }

    /**
     * Mise à jour après feedback.
     */
    public void update(
            String contextKey,
            Integer hour,
            boolean engaged
    ) {

        initContext(contextKey);

        double[] params = contexts.get(contextKey).get(hour);

        params[0] = Math.max(1.0, params[0] * decay);
        params[1] = Math.max(1.0, params[1] * decay);

        if (engaged) {
            params[0] += 1.0;
        } else {
            params[1] += 1.0;
        }

        log.debug(
                "Bandit update: {}@{}h engaged={} α={} β={}",
                contextKey,
                hour,
                engaged,
                params[0],
                params[1]
        );
    }

    /**
     * Détection de dérive.
     */
    private boolean detectDrift(String contextKey) {

        LocalDateTime since = LocalDateTime.now().minusDays(30);

        var logs = decisionLogRepo.findByContextKeyAndEngagedIsNotNullAndDecidedAtGreaterThanEqual(
                contextKey,
                since
        );

        if (logs.size() < 100) {
            return false;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        var recent = logs.stream()
                .filter(log -> log.getDecidedAt().isAfter(cutoff))
                .toList();

        var old = logs.stream()
                .filter(log -> !log.getDecidedAt().isAfter(cutoff))
                .toList();

        if (recent.isEmpty() || old.isEmpty()) {
            return false;
        }

        double recentRate = recent.stream()
                .mapToInt(log ->
                        Boolean.TRUE.equals(log.getEngaged()) ? 1 : 0)
                .average()
                .orElse(0);

        double oldRate = old.stream()
                .mapToInt(log ->
                        Boolean.TRUE.equals(log.getEngaged()) ? 1 : 0)
                .average()
                .orElse(0);

        return recentRate < oldRate * 0.80;
    }

    private void initContext(String contextKey) {

        contexts.computeIfAbsent(contextKey, key -> {

            Map<Integer, double[]> ctx = new HashMap<>();

            HOURS.forEach(hour ->
                    ctx.put(hour, new double[]{1.0, 1.0}));

            return ctx;
        });
    }

    private void resetContext(String contextKey) {

        Map<Integer, double[]> ctx = new HashMap<>();

        HOURS.forEach(hour ->
                ctx.put(hour, new double[]{1.0, 1.0}));

        contexts.put(contextKey, ctx);
    }

    /**
     * Thompson Sampling.
     */
    private double sampleBeta(double alpha, double beta) {

        double x = sampleGamma(alpha);
        double y = sampleGamma(beta);

        return x / (x + y + 1e-10);
    }

    private double sampleGamma(double shape) {

        if (shape < 1.0) {
            return sampleGamma(shape + 1.0)
                    * Math.pow(random.nextDouble(), 1.0 / shape);
        }

        double d = shape - 1.0 / 3.0;
        double c = 1.0 / Math.sqrt(9.0 * d);

        while (true) {

            double x;
            double v;

            do {
                x = random.nextGaussian();
                v = 1.0 + c * x;
            } while (v <= 0);

            v = v * v * v;

            double u = random.nextDouble();

            if (u < 1.0 - 0.0331 * Math.pow(x, 4)) {
                return d * v;
            }

            if (Math.log(u)
                    < 0.5 * x * x
                    + d * (1.0 - v + Math.log(v))) {
                return d * v;
            }
        }
    }

    /**
     * Sauvegarde toutes les 5 minutes.
     */
    @Scheduled(fixedDelay = 300_000)
    public void saveState() {

        try {
            objectMapper.writeValue(
                    new File(statePath),
                    contexts
            );
        } catch (IOException e) {
            log.error("Sauvegarde bandit échouée", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadState() {

        File file = new File(statePath);

        if (!file.exists()) {
            return;
        }

        try {

            Map<String, Map<String, List<Double>>> raw =
                    objectMapper.readValue(file, Map.class);

            raw.forEach((contextKey, values) -> {

                Map<Integer, double[]> ctx = new HashMap<>();

                values.forEach((hour, params) ->
                        ctx.put(
                                Integer.parseInt(hour),
                                new double[]{
                                        params.get(0),
                                        params.get(1)
                                }
                        ));

                contexts.put(contextKey, ctx);
            });

        } catch (IOException e) {
            log.warn("État bandit non chargé, démarrage vierge");
        }
    }

    @Data
    @AllArgsConstructor
    public static class BanditDecision {

        private Integer selectedHour;
        private String method;
        private Double mlScore;
    }
}