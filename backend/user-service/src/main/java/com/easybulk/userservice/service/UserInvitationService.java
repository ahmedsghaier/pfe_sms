package com.easybulk.userservice.service;

import com.easybulk.userservice.dto.InviteUserRequest;
import com.easybulk.userservice.Model.UserInvitation;
import com.easybulk.userservice.repository.UserInvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInvitationService {

    private final UserInvitationRepository invitationRepository;

    public UserInvitation inviteUser(String organizationId, String invitedBy, InviteUserRequest request) {
        log.info("Inviting user: {} to organization: {}", request.getEmail(), organizationId);

        // Vérifier si une invitation existe déjà
        invitationRepository.findByEmail(request.getEmail())
                .ifPresent(existing -> {
                    if (!existing.isUsed()) {
                        throw new RuntimeException("User already invited");
                    }
                });

        List<UserInvitation.GroupRole> groupRoles = request.getGroupRoles().stream()
                .map(gr -> UserInvitation.GroupRole.builder()
                        .groupId(gr.getGroupId())
                        .role(gr.getRole())
                        .build())
                .collect(Collectors.toList());

        UserInvitation invitation = UserInvitation.builder()
                .email(request.getEmail())
                .organizationId(organizationId)
                .groupRoles(groupRoles)
                .invitedBy(invitedBy)
                .verificationToken(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        invitation = invitationRepository.save(invitation);

        // TODO: Envoyer email d'invitation
        log.info("Invitation created with token: {}", invitation.getVerificationToken());

        return invitation;
    }

    public UserInvitation verifyInvitation(String token) {
        UserInvitation invitation = invitationRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid invitation token"));

        if (invitation.isUsed()) {
            throw new RuntimeException("Invitation already used");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invitation expired");
        }

        return invitation;
    }

    public void markAsUsed(String invitationId) {
        UserInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        invitation.setUsed(true);
        invitationRepository.save(invitation);
    }
}
