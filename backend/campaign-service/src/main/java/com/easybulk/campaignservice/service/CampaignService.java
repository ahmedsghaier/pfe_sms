package com.easybulk.campaignservice.service;

import com.easybulk.campaignservice.dto.CampaignStatsResponse;
import com.easybulk.campaignservice.dto.CreateCampaignRequest;
import com.easybulk.campaignservice.dto.ValidateCampaignRequest;
import com.easybulk.campaignservice.model.Campaign;
import com.easybulk.campaignservice.model.SmsLog;
import com.easybulk.campaignservice.repository.CampaignRepository;
import com.easybulk.campaignservice.repository.SmsLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.easybulk.campaignservice.client.AITimingFeignClient;
import com.easybulk.campaignservice.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository    campaignRepository;
    private final SmsLogRepository      smsLogRepository;
    private final WebClient.Builder     webClientBuilder;
    private final AITimingFeignClient   aiTimingClient;

    // ── Constantes de conversion time_score_norm → % ─────────────────────
    private static final double MIN_SCORE = 0.5;
    private static final double MAX_SCORE = 1.3;
    private static final double MIN_RATE  = 30.0;
    private static final double MAX_RATE  = 95.0;

    // ── Table de référence horaire par type NLP ───────────────────────────
    // Utilisée uniquement si le ML ne renvoie pas hourlyScores.
    private static final Map<String, Map<Integer, Double>> REFERENCE_SCORES = new HashMap<>();
    static {
        Map<Integer, Double> otp = new HashMap<>();
        otp.put(0,0.82); otp.put(1,0.78); otp.put(2,0.75); otp.put(3,0.72);
        otp.put(4,0.73); otp.put(5,0.76); otp.put(6,0.85); otp.put(7,0.95);
        otp.put(8,1.08); otp.put(9,1.20); otp.put(10,1.25); otp.put(11,1.22);
        otp.put(12,1.10); otp.put(13,1.05); otp.put(14,1.00); otp.put(15,0.98);
        otp.put(16,0.97); otp.put(17,1.00); otp.put(18,1.10); otp.put(19,1.15);
        otp.put(20,1.05); otp.put(21,0.95); otp.put(22,0.88); otp.put(23,0.84);
        REFERENCE_SCORES.put("OTP", otp);

        Map<Integer, Double> tx = new HashMap<>();
        tx.put(0,0.80); tx.put(1,0.75); tx.put(2,0.72); tx.put(3,0.70);
        tx.put(4,0.71); tx.put(5,0.74); tx.put(6,0.82); tx.put(7,0.92);
        tx.put(8,1.05); tx.put(9,1.18); tx.put(10,1.22); tx.put(11,1.20);
        tx.put(12,1.08); tx.put(13,1.02); tx.put(14,0.98); tx.put(15,0.97);
        tx.put(16,1.00); tx.put(17,1.12); tx.put(18,1.18); tx.put(19,1.10);
        tx.put(20,0.95); tx.put(21,0.88); tx.put(22,0.82); tx.put(23,0.80);
        REFERENCE_SCORES.put("Transaction", tx);

        Map<Integer, Double> promo = new HashMap<>();
        promo.put(0,0.78); promo.put(1,0.72); promo.put(2,0.68); promo.put(3,0.65);
        promo.put(4,0.66); promo.put(5,0.70); promo.put(6,0.80); promo.put(7,0.95);
        promo.put(8,1.10); promo.put(9,1.22); promo.put(10,1.28); promo.put(11,1.25);
        promo.put(12,1.12); promo.put(13,1.05); promo.put(14,1.00); promo.put(15,0.95);
        promo.put(16,0.92); promo.put(17,0.90); promo.put(18,0.88); promo.put(19,0.85);
        promo.put(20,0.80); promo.put(21,0.75); promo.put(22,0.72); promo.put(23,0.75);
        REFERENCE_SCORES.put("Promotion", promo);

        Map<Integer, Double> reminder = new HashMap<>();
        reminder.put(0,0.80); reminder.put(1,0.75); reminder.put(2,0.72); reminder.put(3,0.70);
        reminder.put(4,0.71); reminder.put(5,0.75); reminder.put(6,0.83); reminder.put(7,0.93);
        reminder.put(8,1.05); reminder.put(9,1.12); reminder.put(10,1.15); reminder.put(11,1.10);
        reminder.put(12,1.00); reminder.put(13,0.97); reminder.put(14,0.98); reminder.put(15,1.02);
        reminder.put(16,1.10); reminder.put(17,1.15); reminder.put(18,1.08); reminder.put(19,0.98);
        reminder.put(20,0.90); reminder.put(21,0.85); reminder.put(22,0.80); reminder.put(23,0.78);
        REFERENCE_SCORES.put("Reminder", reminder);

        Map<Integer, Double> info = new HashMap<>();
        info.put(0,0.82); info.put(1,0.78); info.put(2,0.75); info.put(3,0.73);
        info.put(4,0.74); info.put(5,0.77); info.put(6,0.85); info.put(7,0.94);
        info.put(8,1.02); info.put(9,1.08); info.put(10,1.10); info.put(11,1.08);
        info.put(12,1.00); info.put(13,0.98); info.put(14,0.97); info.put(15,0.98);
        info.put(16,1.00); info.put(17,1.05); info.put(18,1.02); info.put(19,0.95);
        info.put(20,0.90); info.put(21,0.85); info.put(22,0.82); info.put(23,0.80);
        REFERENCE_SCORES.put("Information", info);
    }

    // ══════════════════════════════════════════════════════════════════════
    // CAMPAIGN CRUD
    // ══════════════════════════════════════════════════════════════════════

    public Campaign createCampaign(String organizationId, String userId, CreateCampaignRequest request) {
        int messageLength = request.getMessageTemplate().length();
        int numberOfPages = calculateSmsPages(messageLength);
        Campaign campaign = Campaign.builder()
                .organizationId(organizationId).groupId(request.getGroupId())
                .name(request.getName()).type(request.getType())
                .status(Campaign.CampaignStatus.DRAFT).ownerId(userId)
                .messageTemplate(request.getMessageTemplate())
                .messageLength(messageLength).numberOfPages(numberOfPages)
                .contactIds(request.getContactIds() != null ? request.getContactIds() : new ArrayList<>())
                .contactTags(request.getContactTags() != null ? request.getContactTags() : new ArrayList<>())
                .alphaHeader(request.getAlphaHeader())
                .scheduledStartDate(request.getScheduledStartDate())
                .scheduledEndDate(request.getScheduledEndDate())
                .smsValidityHours(request.getSmsValidityHours() != null ? request.getSmsValidityHours() : 48)
                .sendingWindow(request.getSendingWindow() != null ? request.getSendingWindow() : Campaign.SendingWindow.FULL_TIME)
                .testCompleted(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        if (request.getType() == Campaign.CampaignType.TRANSACTIONAL) {
            campaign.setApiKeyId(request.getApiKeyId());
            campaign.setRestrictToApiKey(request.getRestrictToApiKey() != null ? request.getRestrictToApiKey() : false);
        }
        return campaignRepository.save(campaign);
    }

    public Campaign updateCampaign(String campaignId, String userId, CreateCampaignRequest request) {
        Campaign campaign = getCampaignById(campaignId);
        if (!campaign.getOwnerId().equals(userId)) throw new RuntimeException("Not authorized");
        campaign.setName(request.getName());
        campaign.setMessageTemplate(request.getMessageTemplate());
        campaign.setContactIds(request.getContactIds());
        campaign.setContactTags(request.getContactTags());
        campaign.setAlphaHeader(request.getAlphaHeader());
        campaign.setScheduledStartDate(request.getScheduledStartDate());
        campaign.setScheduledEndDate(request.getScheduledEndDate());
        campaign.setUpdatedAt(LocalDateTime.now());
        if (campaign.getStatus() == Campaign.CampaignStatus.PENDING_VALIDATION)
            campaign.setStatus(Campaign.CampaignStatus.IN_MODIFICATION);
        return campaignRepository.save(campaign);
    }

    public Campaign submitForValidation(String campaignId, String userId, String userRole) {
        Campaign campaign = getCampaignById(campaignId);
        int totalRecipients = calculateTotalRecipients(campaign);
        campaign.setTotalRecipients(totalRecipients);
        campaign.setEstimatedCost(calculateEstimatedCost(campaign, totalRecipients));
        campaign.setStatus("AGENT".equals(userRole)
                ? Campaign.CampaignStatus.PENDING_VALIDATION
                : Campaign.CampaignStatus.SCHEDULED);
        campaign.setUpdatedAt(LocalDateTime.now());
        return campaignRepository.save(campaign);
    }

    public Campaign validateCampaign(String campaignId, String validatorId, ValidateCampaignRequest request) {
        Campaign campaign = getCampaignById(campaignId);
        if (campaign.getStatus() != Campaign.CampaignStatus.PENDING_VALIDATION)
            throw new RuntimeException("Campaign is not pending validation");
        if (!campaign.isTestCompleted())
            throw new RuntimeException("Test SMS must be sent before validation");
        if (request.isApproved()) {
            campaign.setStatus(Campaign.CampaignStatus.SCHEDULED);
            campaign.setValidatedBy(validatorId);
            campaign.setValidatedAt(LocalDateTime.now());
        } else {
            campaign.setStatus(Campaign.CampaignStatus.REJECTED);
            campaign.setRejectedBy(validatorId);
            campaign.setRejectedAt(LocalDateTime.now());
            campaign.setRejectionReason(request.getRejectionReason());
        }
        campaign.setUpdatedAt(LocalDateTime.now());
        return campaignRepository.save(campaign);
    }

    public void sendTestSms(String campaignId, String phoneNumber) {
        Campaign campaign = getCampaignById(campaignId);
        campaign.setTestCompleted(true);
        campaign.setTestPhoneNumber(phoneNumber);
        campaign.setUpdatedAt(LocalDateTime.now());
        campaignRepository.save(campaign);
    }

    public Campaign stopCampaign(String campaignId, String userId) {
        Campaign campaign = getCampaignById(campaignId);
        if (campaign.getProgressPercentage() >= 90)
            throw new RuntimeException("Cannot stop campaign - already 90% complete");
        campaign.setStatus(Campaign.CampaignStatus.STOPPED);
        campaign.setUpdatedAt(LocalDateTime.now());
        return campaignRepository.save(campaign);
    }

    public Page<Campaign> getCampaignsByOrganization(String orgId, Pageable p) { return campaignRepository.findByOrganizationId(orgId, p); }
    public Page<Campaign> getCampaignsByGroup(String groupId, Pageable p)      { return campaignRepository.findByGroupId(groupId, p); }
    public Page<Campaign> getCampaignsByOwner(String ownerId, Pageable p)      { return campaignRepository.findByOwnerId(ownerId, p); }
    public Campaign getCampaignById(String id) {
        return campaignRepository.findById(id).orElseThrow(() -> new RuntimeException("Campaign not found"));
    }

    public CampaignStatsResponse getCampaignStats(String campaignId) {
        Campaign c = getCampaignById(campaignId);
        return CampaignStatsResponse.builder()
                .campaignId(c.getId()).campaignName(c.getName())
                .totalRecipients(c.getTotalRecipients()).sentCount(c.getSentCount())
                .deliveredCount(c.getDeliveredCount()).failedCount(c.getFailedCount())
                .pendingCount(c.getPendingCount()).deliveryRate(c.getDeliveryRate())
                .progressPercentage(c.getProgressPercentage())
                .estimatedCost(c.getEstimatedCost()).actualCost(c.getActualCost())
                .build();
    }

    public Page<SmsLog> getCampaignLogs(String campaignId, Pageable pageable) {
        return smsLogRepository.findByCampaignId(campaignId, pageable);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ENGAGEMENT PREDICTION
    // Retourne uniquement ce que le ML retourne — aucune valeur inventée.
    // ══════════════════════════════════════════════════════════════════════

    public EngagementPredictionResponse predictEngagement(EngagementPredictionRequest request) {
        try {
            String messageContent = request.getMessageTemplate();
            String nlpType        = detectNlpType(messageContent);
            String sendingWindow  = (request.getSendingWindow() != null && !request.getSendingWindow().isEmpty())
                    ? request.getSendingWindow() : "ALL_DAY";

            LocalDateTime startDt = parseScheduledDate(request.getStartDate());
            LocalDateTime endDt   = parseScheduledDate(request.getEndDate());
            if (endDt.isBefore(startDt)) {
                log.warn("endDate avant startDate — correction automatique +24h");
                endDt = startDt.plusHours(24);
            }

            // ── Appel ML ─────────────────────────────────────────────────
            TimingPredictionRequest timingRequest = TimingPredictionRequest.builder()
                    .campaignType("CLASSIC").operateur("Orange").nlpType(nlpType)
                    .message(messageContent).messageLength(messageContent.length())
                    .encoding(isArabic(messageContent) ? "UCS2" : "UTF-8")
                    .nbrPages(calculateSmsPages(messageContent.length()))
                    .scheduledDate(startDt).scheduleHourStart(8).scheduleHourEnd(20)
                    .build();

            log.info("Calling AI | nlpType={} | window={} | {} -> {}", nlpType, sendingWindow, startDt, endDt);

            ResponseEntity<String> response = aiTimingClient.predictTiming(timingRequest);
            if (response.getBody() == null) throw new RuntimeException("Empty response from AI service");

            log.info("=== RAW ML RESPONSE === {}", response.getBody());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            // ── bestHour : exactement ce que le ML retourne ───────────────
            // Pas de valeur par défaut → 0 si absent (détectable côté frontend)
            Integer bestHour = root.path("bestHour").isMissingNode()
                    ? null
                    : root.path("bestHour").asInt();
            log.info("ML bestHour={}", bestHour);

            // ── bestWindow : exactement ce que le ML retourne ─────────────
            // Priorité 1 : optimalWindow.label  (ex: "07h–11h")
            // Priorité 2 : optimalWindow.start + end
            // Aucun fallback inventé → null si absent
            String bestWindow = null;
            JsonNode windowNode = root.path("optimalWindow");
            if (!windowNode.isMissingNode() && windowNode.has("label")) {
                bestWindow = windowNode.path("label").asText();
            } else if (!windowNode.isMissingNode() && windowNode.has("start") && windowNode.has("end")) {
                bestWindow = windowNode.path("start").asInt() + "h–" + windowNode.path("end").asInt() + "h";
            } else {
                log.warn("ML ne retourne pas optimalWindow pour nlpType={} — bestWindow=null", nlpType);
            }
            log.info("ML bestWindow={}", bestWindow);

            // ── nlpType retourné par le ML ────────────────────────────────
            String detectedType = root.path("metadata").path("nlp_type").asText(nlpType);

            // ── Scores horaires (ML ou table de référence) ────────────────
            Map<Integer, Double> hourlyScores = extractHourlyScores(root, nlpType);

            // ── Taux de la fenêtre CHOISIE par l'utilisateur ─────────────
            double engagementRate = computeWindowRate(hourlyScores, sendingWindow, startDt, endDt);
            log.info("Fenêtre choisie={} → rate={}%", sendingWindow, String.format("%.1f", engagementRate));

            // ── Score de l'heure exacte de début ─────────────────────────
            Double chosenHourScore = null;
            try {
                int    h     = startDt.getHour();
                double score = hourlyScores.getOrDefault(h, 1.0);
                chosenHourScore = scoreToRate(score);
                log.info("Heure de début {}h → {}%", h, String.format("%.1f", chosenHourScore));
            } catch (Exception e) {
                log.warn("Could not compute chosenHourScore: {}", e.getMessage());
            }

            // ── TOP 2 fenêtres recommandées ───────────────────────────────
            List<EngagementPredictionResponse.WindowScore> topWindows =
                    computeTopWindows(hourlyScores, startDt, endDt);

            // ── Recommandation ────────────────────────────────────────────
            int[] range = getHourRange(sendingWindow);
            List<Integer> activeHours = computeActiveHours(sendingWindow, startDt, endDt);
            String recommendation = buildRecommendation(
                    engagementRate, bestHour, bestWindow, chosenHourScore,
                    sendingWindow, range[0], range[1], activeHours.size(),
                    topWindows.isEmpty() ? null : topWindows.get(0));

            return EngagementPredictionResponse.builder()
                    .nlpType(detectedType)
                    .predictedEngagementRate(engagementRate)
                    .bestHour(bestHour)           // Integer nullable
                    .bestWindow(bestWindow)        // null si ML ne retourne pas
                    .chosenHourScore(chosenHourScore)
                    .recommendation(recommendation)
                    .topWindows(topWindows)
                    .build();

        } catch (Exception e) {
            log.error("Error during engagement prediction", e);
            return buildFallbackResponse();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS — SCORES ET TAUX
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Extrait hourlyScores depuis la réponse ML.
     * Priorité : hourlyScores ML → table REFERENCE_SCORES par nlpType → predictionScore global.
     * Note : hourlyScores sert au calcul des taux de fenêtres, pas à bestWindow.
     */
    private Map<Integer, Double> extractHourlyScores(JsonNode root, String nlpType) {
        JsonNode scoresNode = root.path("hourlyScores");
        if (!scoresNode.isMissingNode() && scoresNode.isObject() && scoresNode.size() > 0) {
            Map<Integer, Double> scores = new HashMap<>();
            scoresNode.fields().forEachRemaining(e -> {
                try { scores.put(Integer.parseInt(e.getKey()), e.getValue().asDouble()); }
                catch (NumberFormatException ex) { /* ignorer clé invalide */ }
            });
            if (!scores.isEmpty()) {
                log.info("Source hourlyScores: ML ({} entrées)", scores.size());
                return scores;
            }
        }
        if (REFERENCE_SCORES.containsKey(nlpType)) {
            log.info("Source hourlyScores: table de référence nlpType={}", nlpType);
            return new HashMap<>(REFERENCE_SCORES.get(nlpType));
        }
        double global = root.path("predictionScore").asDouble(1.0);
        log.warn("Source hourlyScores: predictionScore global={} (flat)", global);
        Map<Integer, Double> flat = new HashMap<>();
        for (int h = 0; h < 24; h++) flat.put(h, global);
        return flat;
    }

    /**
     * Calcule le taux d'engagement pour une fenêtre donnée sur la plage startDt→endDt.
     */
    private double computeWindowRate(Map<Integer, Double> hourlyScores,
                                     String sendingWindow,
                                     LocalDateTime startDt,
                                     LocalDateTime endDt) {
        int[] range = getHourRange(sendingWindow);
        List<Integer> active = computeActiveHours(sendingWindow, startDt, endDt);

        double avgScore;
        if (!active.isEmpty()) {
            avgScore = active.stream()
                    .mapToDouble(h -> hourlyScores.getOrDefault(h, 1.0))
                    .average().orElse(1.0);
        } else {
            log.warn("Aucune heure active pour {} — fallback plage statique {}h-{}h",
                    sendingWindow, range[0], range[1]);
            avgScore = IntStream.rangeClosed(range[0], range[1])
                    .mapToDouble(h -> hourlyScores.getOrDefault(h, 1.0))
                    .average().orElse(1.0);
        }
        return scoreToRate(avgScore);
    }

    /**
     * Calcule les taux des 3 fenêtres et retourne les 2 meilleures triées par taux décroissant.
     */
    private List<EngagementPredictionResponse.WindowScore> computeTopWindows(
            Map<Integer, Double> hourlyScores,
            LocalDateTime startDt,
            LocalDateTime endDt) {

        record WindowDef(String key, String label, String icon) {}

        List<WindowDef> defs = List.of(
                new WindowDef("BUSINESS_HOURS", "Jours ouvrables (08h–18h)", "💼"),
                new WindowDef("EVENING",        "Plage du soir (18h–22h)",   "🌙"),
                new WindowDef("ALL_DAY",        "Tous les jours (24h/24)",   "🌐")
        );

        List<EngagementPredictionResponse.WindowScore> results = new ArrayList<>();

        for (WindowDef def : defs) {
            int[] range = getHourRange(def.key());
            double rate = computeWindowRate(hourlyScores, def.key(), startDt, endDt);

            final int rs = range[0], re = range[1];
            int peakHour = IntStream.rangeClosed(rs, re)
                    .boxed()
                    .max(Comparator.comparingDouble(h -> hourlyScores.getOrDefault(h, 0.0)))
                    .orElse(rs);

            results.add(EngagementPredictionResponse.WindowScore.builder()
                    .windowKey(def.key())
                    .windowLabel(def.label())
                    .rate(rate)
                    .peakHour(peakHour)
                    .icon(def.icon())
                    .build());

            log.info("TopWindows calc: {} → {}% (pic {}h)", def.key(),
                    String.format("%.1f", rate), peakHour);
        }

        results.sort((a, b) -> Double.compare(b.getRate(), a.getRate()));
        return results.subList(0, Math.min(2, results.size()));
    }

    /**
     * Calcule les heures actives = intersection(sendingWindow, startDt→endDt).
     */
    private List<Integer> computeActiveHours(String sendingWindow,
                                             LocalDateTime startDt,
                                             LocalDateTime endDt) {
        int[] range  = getHourRange(sendingWindow);
        int winStart = range[0], winEnd = range[1];

        List<Integer> activeHours = new ArrayList<>();
        LocalDateTime cursor = startDt.withMinute(0).withSecond(0).withNano(0);

        if (startDt.getMinute() > 0 || startDt.getSecond() > 0) cursor = cursor.plusHours(1);
        if (cursor.getHour() < winStart) cursor = cursor.withHour(winStart);

        int maxIter = 720, iter = 0;
        while (!cursor.isAfter(endDt) && iter < maxIter) {
            int h = cursor.getHour();
            if (h >= winStart && h <= winEnd) {
                activeHours.add(h);
                cursor = cursor.plusHours(1);
            } else if (h > winEnd) {
                cursor = cursor.plusDays(1).withHour(winStart).withMinute(0).withSecond(0);
            } else {
                cursor = cursor.withHour(winStart).withMinute(0).withSecond(0);
            }
            iter++;
        }
        log.debug("computeActiveHours({}) {} → {} = {} h", sendingWindow, startDt, endDt, activeHours.size());
        return activeHours;
    }

    /** Convertit un time_score_norm en pourcentage d'engagement. */
    private double scoreToRate(double score) {
        double rate = MIN_RATE + ((score - MIN_SCORE) / (MAX_SCORE - MIN_SCORE)) * (MAX_RATE - MIN_RATE);
        return Math.min(MAX_RATE, Math.max(MIN_RATE, rate));
    }

    /** Plage horaire [start, end] par sendingWindow. */
    private int[] getHourRange(String sendingWindow) {
        return switch (sendingWindow) {
            case "BUSINESS_HOURS" -> new int[]{8, 18};
            case "EVENING"        -> new int[]{18, 22};
            default               -> new int[]{0, 23};
        };
    }

    private String getSendingWindowLabel(String sendingWindow) {
        return switch (sendingWindow) {
            case "BUSINESS_HOURS" -> "Jours ouvrables (08h–18h)";
            case "EVENING"        -> "Plage du soir (18h–22h)";
            default               -> "Tous les jours (24h/24)";
        };
    }

    /**
     * Construit la recommandation.
     * bestHour et bestWindow peuvent être null si le ML ne les retourne pas.
     */
    private String buildRecommendation(double engagementRate,
                                       Integer bestHour,
                                       String bestWindow,
                                       Double chosenHourScore,
                                       String sendingWindow,
                                       int rangeStart, int rangeEnd,
                                       int activeHoursCount,
                                       EngagementPredictionResponse.WindowScore bestTopWindow) {
        StringBuilder rec = new StringBuilder();

        if      (engagementRate >= 75) rec.append("Excellent potentiel d'engagement ! ");
        else if (engagementRate >= 60) rec.append("Bon potentiel d'engagement. ");
        else if (engagementRate >= 45) rec.append("Potentiel d'engagement modere. ");
        else                           rec.append("Potentiel d'engagement faible. ");

        rec.append(String.format(
                "Taux moyen sur %d creneaux actifs (%s) : %.1f%%. ",
                activeHoursCount, getSendingWindowLabel(sendingWindow), engagementRate));

        // Affiche le pic uniquement si le ML l'a retourné
        if (bestHour != null && bestWindow != null) {
            rec.append(String.format("Pic optimal ML : %dh00 (%s). ", bestHour, bestWindow));
        } else if (bestHour != null) {
            rec.append(String.format("Pic optimal ML : %dh00. ", bestHour));
        }

        // Suggestion de changer de fenêtre si une meilleure existe
        if (bestTopWindow != null
                && !bestTopWindow.getWindowKey().equals(sendingWindow)
                && bestTopWindow.getRate() > engagementRate + 5) {
            rec.append(String.format(
                    "Conseil : passer a « %s » pourrait augmenter votre taux a %.1f%%.",
                    bestTopWindow.getWindowLabel(), bestTopWindow.getRate()));
        }

        if (chosenHourScore != null) {
            if      (chosenHourScore >= 80) rec.append(" L'heure de debut est excellente !");
            else if (chosenHourScore >= 60) rec.append(" L'heure de debut est correcte mais pas au pic.");
            else if (bestHour != null)
                rec.append(String.format(" Deplacez l'heure de debut vers %dh pour plus d'impact.", bestHour));
        }

        return rec.toString();
    }

    /**
     * Réponse en cas d'erreur ML.
     * Retourne null pour bestHour, bestWindow, predictedEngagementRate
     * et une liste vide pour topWindows — aucune valeur inventée.
     */
    private EngagementPredictionResponse buildFallbackResponse() {
        return EngagementPredictionResponse.builder()
                .nlpType(null)
                .predictedEngagementRate(null)   // Double nullable
                .bestHour(null)                  // Integer nullable
                .bestWindow(null)
                .chosenHourScore(null)
                .recommendation("Service ML indisponible. Veuillez reessayer.")
                .topWindows(List.of())           // liste vide, pas de fausses données
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS — DÉTECTION / PARSING
    // ══════════════════════════════════════════════════════════════════════

    private String detectNlpType(String message) {
        String lower = message.toLowerCase();
        if      (lower.contains("otp")          || lower.contains("code"))       return "OTP";
        else if (lower.contains("transaction")  || lower.contains("paiement"))   return "Transaction";
        else if (lower.contains("promo")        || lower.contains("offre")
                || lower.contains("reduction"))                                  return "Promotion";
        else if (lower.contains("rappel")       || lower.contains("rendez-vous")) return "Reminder";
        else                                                                      return "Information";
    }

    private LocalDateTime parseScheduledDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return LocalDateTime.now().plusHours(1);
        try {
            if (dateString.contains("T"))  return LocalDateTime.parse(dateString);
            if (dateString.contains(" ") && dateString.length() > 10)
                return LocalDateTime.parse(dateString.replace(" ", "T"));
            return LocalDateTime.parse(dateString + "T00:00:00");
        } catch (Exception e) {
            log.warn("Could not parse date '{}', using default", dateString);
            return LocalDateTime.now().plusHours(1);
        }
    }

    private boolean isArabic(String message) {
        return message.chars().anyMatch(c -> c >= 0x0600 && c <= 0x06FF);
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS — CALCULS CAMPAGNE
    // ══════════════════════════════════════════════════════════════════════

    private int calculateSmsPages(int len) { return (int) Math.ceil((double) len / 160); }

    private int calculateTotalRecipients(Campaign campaign) {
        int total = 0;
        if (campaign.getContactIds() != null) total += campaign.getContactIds().size();
        if (campaign.getContactTags() != null && !campaign.getContactTags().isEmpty()) total += 100;
        return total;
    }

    private BigDecimal calculateEstimatedCost(Campaign campaign, int totalRecipients) {
        return new BigDecimal("0.05").multiply(BigDecimal.valueOf((long) totalRecipients * campaign.getNumberOfPages()));
    }
}