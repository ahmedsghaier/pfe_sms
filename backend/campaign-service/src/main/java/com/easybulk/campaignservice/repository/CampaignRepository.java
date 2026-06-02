package com.easybulk.campaignservice.repository;


import com.easybulk.campaignservice.model.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignRepository extends MongoRepository<Campaign, String> {

    Page<Campaign> findByOrganizationId(String organizationId, Pageable pageable);

    Page<Campaign> findByGroupId(String groupId, Pageable pageable);

    Page<Campaign> findByOwnerId(String ownerId, Pageable pageable);

    List<Campaign> findByStatus(Campaign.CampaignStatus status);

    List<Campaign> findByStatusAndScheduledStartDateBefore(
            Campaign.CampaignStatus status, LocalDateTime date);

    long countByGroupIdAndStatus(String groupId, Campaign.CampaignStatus status);
}
