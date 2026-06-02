package com.easybulk.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class InviteUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotEmpty(message = "At least one group role is required")
    private List<GroupRoleDto> groupRoles;

    @Data
    public static class GroupRoleDto {
        @NotBlank(message = "Group ID is required")
        private String groupId;

        @NotBlank(message = "Role is required")
        private String role; // ADMIN, SUPERVISOR, AGENT, VIEWER
    }
}
