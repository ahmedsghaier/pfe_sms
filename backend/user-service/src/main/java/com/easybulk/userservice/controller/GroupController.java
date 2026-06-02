package com.easybulk.userservice.controller;

import com.easybulk.common.dto.ApiResponse;
import com.easybulk.userservice.dto.CreateGroupRequest;
import com.easybulk.userservice.Model.Group;
import com.easybulk.userservice.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<ApiResponse<Group>> createGroup(
            @RequestHeader("X-Organization-Id") String organizationId,
            @Valid @RequestBody CreateGroupRequest request) {

        Group group = groupService.createGroup(organizationId, request);
        return ResponseEntity.ok(ApiResponse.success("Group created successfully", group));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Group>>> getGroups(
            @RequestHeader("X-Organization-Id") String organizationId) {

        List<Group> groups = groupService.getGroupsByOrganization(organizationId);
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Group>> getGroup(@PathVariable String groupId) {
        Group group = groupService.getGroupById(groupId);
        return ResponseEntity.ok(ApiResponse.success(group));
    }

    @PatchMapping("/{groupId}/budget")
    public ResponseEntity<ApiResponse<Group>> addBudget(
            @PathVariable String groupId,
            @RequestParam BigDecimal amount) {

        Group group = groupService.addBudget(groupId, amount);
        return ResponseEntity.ok(ApiResponse.success("Budget added successfully", group));
    }

    @PatchMapping("/{groupId}/deactivate")
    public ResponseEntity<ApiResponse<Group>> deactivateGroup(@PathVariable String groupId) {
        Group group = groupService.deactivateGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success("Group deactivated", group));
    }
}
