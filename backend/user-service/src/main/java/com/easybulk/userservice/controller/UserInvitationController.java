package com.easybulk.userservice.controller;

import com.easybulk.common.dto.ApiResponse;
import com.easybulk.userservice.dto.InviteUserRequest;
import com.easybulk.userservice.Model.UserInvitation;
import com.easybulk.userservice.service.UserInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/invitations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserInvitationController {

    private final UserInvitationService invitationService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserInvitation>> inviteUser(
            @RequestHeader("X-Organization-Id") String organizationId,
            @RequestHeader("X-User-Id") String invitedBy,
            @Valid @RequestBody InviteUserRequest request) {

        UserInvitation invitation = invitationService.inviteUser(organizationId, invitedBy, request);
        return ResponseEntity.ok(ApiResponse.success("Invitation sent successfully", invitation));
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<ApiResponse<UserInvitation>> verifyInvitation(@PathVariable String token) {
        UserInvitation invitation = invitationService.verifyInvitation(token);
        return ResponseEntity.ok(ApiResponse.success(invitation));
    }
}