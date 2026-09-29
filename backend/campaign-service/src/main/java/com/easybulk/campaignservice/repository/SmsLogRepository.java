package com.easybulk.campaignservice.repository;


import com.easybulk.campaignservice.model.SmsLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmsLogRepository extends MongoRepository<SmsLog, String> {

    Page<SmsLog> findByCampaignId(String campaignId, Pageable pageable);

    long countByCampaignIdAndStatus(String campaignId, SmsLog.SmsStatus status);

    List<SmsLog> findByCampaignIdAndStatus(String campaignId, SmsLog.SmsStatus status);

}
