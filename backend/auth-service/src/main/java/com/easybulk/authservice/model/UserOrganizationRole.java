package com.easybulk.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_organization_roles")
public class UserOrganizationRole {

    @Id
    private String id;

    private String userId;

    private String organizationId;

    private String groupId; // peut être null pour Superadmin

    private Role role;

    public enum Role {
        SUPERADMIN,
        ADMIN,
        SUPERVISOR,
        AGENT,
        VIEWER
    }
}
