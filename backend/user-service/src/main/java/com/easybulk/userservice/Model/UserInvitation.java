package com.easybulk.userservice.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_invitations")
public class UserInvitation {

    @Id
    private String id;

    private String email;

    private String organizationId;

    private List<GroupRole> groupRoles;

    private String invitedBy;

    private String verificationToken;

    private LocalDateTime expiresAt;

    private boolean used;

    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupRole {
        private String groupId;
        private String role; // ADMIN, SUPERVISOR, AGENT, VIEWER
    }
}
