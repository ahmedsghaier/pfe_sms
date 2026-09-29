package com.easybulk.service;

import com.easybulk.client.PythonMlClient;
import com.easybulk.dto.FeedbackLog;
import com.easybulk.dto.FeedbackRequest;
import com.easybulk.dto.RecommendRequest;
import com.easybulk.dto.RecommendResponse;
import com.easybulk.repository.FeedbackLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class SmsRecommenderService {

    private final PythonMlClient mlClient;
    private final FeedbackLogRepository feedbackRepo;
    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;

    @Value("${recommender.cache-ttl-seconds:300}")
    private long cacheTtlSeconds;

    @Autowired
    public SmsRecommenderService(
            PythonMlClient mlClient,
            FeedbackLogRepository feedbackRepo,
            @Qualifier("redisTemplate") RedisTemplate<String, String> redis,
            ObjectMapper objectMapper) {
        this.mlClient     = mlClient;
        this.feedbackRepo = feedbackRepo;
        this.redis        = redis;
        this.objectMapper = objectMapper;
    }

    public RecommendResponse recommend(RecommendRequest req) {
        String cacheKey = "sms:rec:" + req.getPartialText().hashCode() + ":" + req.getTopK();

        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, RecommendResponse.class);
            } catch (Exception e) {
                log.warn("Cache deserialize error", e);
            }
        }

        RecommendResponse response = mlClient.recommend(req);

        try {
            redis.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(response),
                    Duration.ofSeconds(cacheTtlSeconds)
            );
        } catch (Exception e) {
            log.warn("Cache write error", e);
        }

        return response;
    }

    public void recordFeedback(FeedbackRequest req) {
        FeedbackLog entry = new FeedbackLog();
        entry.setPartialText(req.getPartialText());
        entry.setSelectedWord(req.getSelectedWord());
        entry.setUserId(req.getUserId());
        feedbackRepo.save(entry);

        try {
            mlClient.feedback(req);
        } catch (Exception e) {
            log.warn("Python ML unreachable, feedback logged only: {}", e.getMessage());
        }
    }
}