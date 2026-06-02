package com.easybulk.campaignservice.repository;

import com.easybulk.campaignservice.model.MessageTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageTemplateRepository extends MongoRepository<MessageTemplate, String> {
    List<MessageTemplate> findByUserIdAndActive(String userId, boolean active);
    List<MessageTemplate> findByOrganizationIdAndActive(String organizationId, boolean active);
    List<MessageTemplate> findByActiveTrue();
    List<MessageTemplate> findByTypeAndActiveTrue(MessageTemplate.TemplateType type);
}