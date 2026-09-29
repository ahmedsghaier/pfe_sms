package com.easybulk.repository;

import com.easybulk.dto.FeedbackLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackLogRepository extends MongoRepository<FeedbackLog, String> {
    List<FeedbackLog> findByCreatedAtAfter(LocalDateTime date);
}