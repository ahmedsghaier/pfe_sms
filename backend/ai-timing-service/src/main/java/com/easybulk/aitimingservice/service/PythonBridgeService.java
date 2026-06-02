package com.easybulk.aitimingservice.service;

import com.easybulk.aitimingservice.dto.FeatureVector;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

@Slf4j
@Service
public class PythonBridgeService {

    private final ObjectMapper objectMapper;
    private List<Map<String, Object>> timingResults;

    public PythonBridgeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        loadResultsFromJson();
    }

    private void loadResultsFromJson() {
        try {
            // Essayer les deux fichiers JSON disponibles
            InputStream is = getClass().getResourceAsStream("/models/resultats_phase2_v13.json");
            if (is == null) {
                is = getClass().getResourceAsStream("/models/resultats_phase2_v13_ts.json");
            }
            // Fallback : lire depuis /app/models/ (copié par Docker)
            if (is == null) {
                File f = new File("/app/models/resultats_phase2_v13.json");
                if (f.exists()) is = new FileInputStream(f);
            }
            if (is == null) {
                File f = new File("/app/models/resultats_phase2_v13_ts.json");
                if (f.exists()) is = new FileInputStream(f);
            }

            if (is == null) {
                log.warn("⚠️ Aucun JSON trouvé — utilisation fallback uniquement");
                timingResults = new ArrayList<>();
                return;
            }

            Map<String, Object> root = objectMapper.readValue(is, Map.class);
            timingResults = (List<Map<String, Object>>) root.get("timing_results");
            log.info("✅ JSON chargé : {} combinaisons timing", timingResults.size());

        } catch (Exception e) {
            log.error("❌ Erreur chargement JSON", e);
            timingResults = new ArrayList<>();
        }
    }

    public Map<String, Object> predict(String campaignType,
                                       String operateur,
                                       String nlpType,
                                       FeatureVector features) {

        // Détecter le type NLP via le modèle Python léger
        String detectedType = detectNlpViaModel(features);

        log.info("NLP détecté par modèle: {} pour message: '{}'",
                detectedType,
                features != null && features.getMessage() != null
                        ? features.getMessage().substring(0, Math.min(50, features.getMessage().length()))
                        : "");

        Map<String, Object> match = findMatch(campaignType, operateur, detectedType);
        return buildPredictionMap(match, detectedType);
    }

    private String detectNlpViaModel(FeatureVector features) {

        if (features == null || features.getMessage() == null) {
            return "Information";
        }

        try {

            String message = features.getMessage();

            String inputJson = objectMapper.writeValueAsString(
                    Map.of("message", message)
            );

            File tempInput = File.createTempFile("nlp_input_", ".json");
            File tempOutput = File.createTempFile("nlp_output_", ".json");

            try {

                try (FileWriter w = new FileWriter(tempInput)) {
                    w.write(inputJson);
                }

                // ✅ utiliser la variable Spring
                File pythonExecutable = null;
                CommandLine cmd = new CommandLine(pythonExecutable);

                cmd.addArgument("/app/python/nlp_detector.py");
                cmd.addArgument(tempInput.getAbsolutePath());
                cmd.addArgument(tempOutput.getAbsolutePath());

                DefaultExecutor executor = new DefaultExecutor();

                executor.setWatchdog(new ExecuteWatchdog(10_000));

                executor.execute(cmd);

                String result = new String(
                        Files.readAllBytes(tempOutput.toPath())
                );

                Map<String, Object> parsed =
                        objectMapper.readValue(result, Map.class);

                return (String) parsed.getOrDefault(
                        "nlp_type",
                        "Information"
                );

            } finally {

                tempInput.delete();
                tempOutput.delete();
            }

        } catch (Exception e) {

            log.warn(
                    "NLP detection failed, using fallback rules: {}",
                    e.getMessage()
            );

            return detectFromRules(features.getMessage());
        }
    }
    private String detectFromRules(String message) {
        if (message == null) return "Information";
        String low = message.toLowerCase();
        if (low.matches(".*\\b(otp|code|vérif|pin)\\b.*"))                          return "OTP";
        if (low.matches(".*\\b(virement|paiement|transaction|solde|dt\\b|débit)\\b.*")) return "Transaction";
        if (low.matches(".*\\b(alerte|suspect|sécurité|détecté)\\b.*"))             return "Alerte";
        if (low.matches(".*\\b(promo|offre|réduction|gratuit|remise)\\b.*"))        return "Promotion";
        if (low.matches(".*\\b(colis|livraison|tracking|suivi)\\b.*"))              return "Livraison";
        if (low.matches(".*\\b(rappel|rendez-vous|rdv|demain)\\b.*"))               return "Rappel";
        return "Information";
    }

    private Map<String, Object> findMatch(String campaignType, String operateur, String nlpType) {
        if (timingResults == null || timingResults.isEmpty()) {
            return null;
        }

        // Match exact
        Optional<Map<String, Object>> exact = timingResults.stream()
                .filter(r ->
                        campaignType.equalsIgnoreCase((String) r.get("camp_type")) &&
                                operateur.equalsIgnoreCase((String) r.get("operateur")) &&
                                nlpType.equalsIgnoreCase((String) r.get("nlp_type")))
                .findFirst();

        if (exact.isPresent()) return exact.get();

        // Fallback par nlpType seulement
        return timingResults.stream()
                .filter(r -> nlpType.equalsIgnoreCase((String) r.get("nlp_type")))
                .findFirst()
                .orElse(null);
    }

    /**
     * Construit la Map avec EXACTEMENT la structure attendue par TimingPredictionService.buildResponse()
     */
    private Map<String, Object> buildPredictionMap(Map<String, Object> match, String nlpType) {

        int bestHour, windowStart, windowEnd;
        double hybridScore, normPeak;
        String method, windowLabel;
        List<Map<String, Object>> hourlyCurve;

        if (match != null) {
            bestHour    = toInt(match.get("best_hour"));
            windowStart = toInt(match.get("window_start"));
            windowEnd   = toInt(match.get("window_end"));
            hybridScore = toDouble(match.get("hybrid_score_peak"));
            normPeak    = toDouble(match.get("norm_peak"));
            method      = (String) match.getOrDefault("method", "precomputed");
            windowLabel = (String) match.getOrDefault("window_label",
                    windowStart + "h–" + windowEnd + "h");
            hourlyCurve = buildHourlyCurve(match, windowStart, windowEnd);
        } else {
            // Fallback total
            bestHour    = getDefaultHour(nlpType);
            windowStart = bestHour - 1;
            windowEnd   = bestHour + 1;
            hybridScore = 1.05;
            normPeak    = 1.05;
            method      = "fallback_default";
            windowLabel = windowStart + "h–" + windowEnd + "h";
            hourlyCurve = buildDefaultHourlyCurve(bestHour);
        }

        double engagementRate = Math.min(95.0, hybridScore * 75.0 + 20.0);

        // Structure EXACTE attendue par TimingPredictionService.buildResponse()
        Map<String, Object> result = new HashMap<>();
        result.put("best_hour", bestHour);
        result.put("hybrid_score_peak", hybridScore);
        result.put("predicted_engagement_rate", Math.round(engagementRate * 10.0) / 10.0);
        result.put("method", method);
        result.put("alpha", 0.5);
        result.put("threshold_pct", 0.95);
        result.put("hourly_curve", hourlyCurve);

        // ← Clé critique attendue par buildResponse() ligne 88
        Map<String, Object> optimalWindow = new HashMap<>();
        optimalWindow.put("start_hour", windowStart);
        optimalWindow.put("end_hour", windowEnd);
        optimalWindow.put("label", windowLabel);
        result.put("optimal_window", optimalWindow);

        return result;
    }

    private List<Map<String, Object>> buildHourlyCurve(
            Map<String, Object> match, int windowStart, int windowEnd) {

        // Utiliser la courbe du JSON si disponible
        List<Map<String, Object>> rawCurve =
                (List<Map<String, Object>>) match.get("hourly_curve");

        if (rawCurve != null && !rawCurve.isEmpty()) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> point : rawCurve) {
                int h = toInt(point.get("hour"));
                Map<String, Object> p = new HashMap<>();
                p.put("hour", h);
                p.put("time_score_raw",  toDouble(point.get("time_score_raw")));
                p.put("time_score_norm", toDouble(point.get("time_score_norm")));
                // Le JSON a "hybrid_score_norm", le service attend "hybrid_score"
                p.put("hybrid_score",    toDouble(point.getOrDefault(
                        "hybrid_score_norm", point.get("hybrid_score"))));
                p.put("in_window", h >= windowStart && h <= windowEnd);
                result.add(p);
            }
            return result;
        }

        return buildDefaultHourlyCurve(toInt(match.get("best_hour")));
    }

    private List<Map<String, Object>> buildDefaultHourlyCurve(int bestHour) {
        List<Map<String, Object>> curve = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            double score = 1.0 + 0.1 * Math.exp(-0.5 * Math.pow(h - bestHour, 2));
            Map<String, Object> p = new HashMap<>();
            p.put("hour", h);
            p.put("time_score_raw",  Math.round(score * 1000.0) / 1000.0);
            p.put("time_score_norm", Math.round(score * 1000.0) / 1000.0);
            p.put("hybrid_score",    Math.round(score * 1000.0) / 1000.0);
            p.put("in_window", false);
            curve.add(p);
        }
        return curve;
    }

    private int getDefaultHour(String nlpType) {
        return switch (nlpType) {
            case "OTP", "Transaction", "Alerte" -> 10;
            case "Promotion", "Livraison"        -> 19;
            case "Rappel"                        -> 9;
            default                              -> 10;
        };
    }

    private int toInt(Object val) {
        if (val instanceof Integer i) return i;
        if (val instanceof Number n)  return n.intValue();
        return 0;
    }

    private double toDouble(Object val) {
        if (val instanceof Double d)  return d;
        if (val instanceof Number n)  return n.doubleValue();
        return 0.0;
    }
}