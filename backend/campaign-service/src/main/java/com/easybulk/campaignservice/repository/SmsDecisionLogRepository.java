package com.easybulk.campaignservice.repository;

import com.easybulk.campaignservice.model.SmsDecisionLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmsDecisionLogRepository extends MongoRepository<SmsDecisionLog, String> {

    Optional<SmsDecisionLog> findBySmsId(String smsId);

    List<SmsDecisionLog> findByContextKeyAndEngagedIsNotNullAndDecidedAtGreaterThanEqual(
            String contextKey,
            LocalDateTime since
    );

    List<SmsDecisionLog> findByOrganizationIdAndDecidedAtGreaterThanEqual(
            String organizationId,
            LocalDateTime since
    );

    List<SmsDecisionLog> findByContextKeyAndEngagedIsNotNull(
            String contextKey
    );

    List<SmsDecisionLog> findByEngagedIsNullAndDecidedAtLessThan(
            LocalDateTime cutoff
    );
}