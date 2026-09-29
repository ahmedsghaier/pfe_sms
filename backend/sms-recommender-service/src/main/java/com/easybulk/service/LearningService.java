package com.easybulk.service;

import com.easybulk.client.PythonMlClient;
import com.easybulk.dto.FeedbackRequest;
import com.easybulk.dto.FeedbackLog;
import com.easybulk.repository.FeedbackLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningService {

    private final FeedbackLogRepository feedbackRepo;
    private final PythonMlClient        mlClient;
    private final RedisTemplate<String, String> redis;

    public void learn(FeedbackRequest req) {
        // 1. Persister dans MongoDB
        FeedbackLog entry = new FeedbackLog();
        entry.setPartialText(req.getPartialText());
        entry.setSelectedWord(req.getSelectedWord());
        entry.setUserId(req.getUserId());
        feedbackRepo.save(entry);

        // 2. Propager au service Python (non-bloquant)
        try {
            mlClient.feedback(req);
        } catch (Exception e) {
            log.warn("[LearningService] Python unreachable, feedback logged only: {}", e.getMessage());
        }
    }

    // Boucle MongoDB → Engines : toutes les heures
    @Scheduled(fixedDelay = 3_600_000)
    public void retrain() {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        List<FeedbackLog> recentLogs = feedbackRepo.findByCreatedAtAfter(since);

        if (recentLogs.isEmpty()) return;

        log.info("[LearningService] Ré-entraînement : {} feedbacks", recentLogs.size());

        try {
            // Envoyer les feedbacks récents au service Python
            recentLogs.forEach(l -> {
                FeedbackRequest req = new FeedbackRequest();
                req.setPartialText(l.getPartialText());
                req.setSelectedWord(l.getSelectedWord());
                mlClient.feedback(req);
            });

            // Invalider le cache Redis (les scores ont changé)
            Set<String> keys = redis.keys("sms:rec:*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
                log.info("[LearningService] Cache Redis invalidé : {} clés supprimées", keys.size());
            }
        } catch (Exception e) {
            log.error("[LearningService] Erreur ré-entraînement : {}", e.getMessage());
        }
    }
}