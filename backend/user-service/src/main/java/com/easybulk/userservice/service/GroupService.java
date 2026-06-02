package com.easybulk.userservice.service;

import com.easybulk.userservice.dto.CreateGroupRequest;
import com.easybulk.userservice.Model.Group;
import com.easybulk.userservice.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    public Group createGroup(String organizationId, CreateGroupRequest request) {
        log.info("Creating group: {} for organization: {}", request.getName(), organizationId);

        Group group = Group.builder()
                .organizationId(organizationId)
                .name(request.getName())
                .description(request.getDescription())
                .budget(request.getBudget())
                .usedBudget(BigDecimal.ZERO)
                .alphaHeader(request.getAlphaHeader())
                .allowedCampaignType(request.getAllowedCampaignType())
                .adminId(request.getAdminId())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return groupRepository.save(group);
    }

    public List<Group> getGroupsByOrganization(String organizationId) {
        return groupRepository.findByOrganizationId(organizationId);
    }

    public Group getGroupById(String groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    public Group addBudget(String groupId, BigDecimal amount) {
        Group group = getGroupById(groupId);
        group.setBudget(group.getBudget().add(amount));
        group.setUpdatedAt(LocalDateTime.now());

        log.info("Added {} to group {} budget. New budget: {}",
                amount, groupId, group.getBudget());

        return groupRepository.save(group);
    }

    public Group deactivateGroup(String groupId) {
        Group group = getGroupById(groupId);
        group.setActive(false);
        group.setUpdatedAt(LocalDateTime.now());

        log.warn("Deactivated group: {} - All campaigns will be stopped!", groupId);

        return groupRepository.save(group);
    }

    public boolean hasAvailableBudget(String groupId, BigDecimal requiredAmount) {
        Group group = getGroupById(groupId);
        return group.getRemainingBudget().compareTo(requiredAmount) >= 0;
    }

    public void consumeBudget(String groupId, BigDecimal amount) {
        Group group = getGroupById(groupId);
        group.setUsedBudget(group.getUsedBudget().add(amount));
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);

        log.info("Consumed {} from group {} budget. Remaining: {}",
                amount, groupId, group.getRemainingBudget());
    }
}