package com.easybulk.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String firstName;

    private String lastName;

    private boolean active;

    private boolean emailVerified;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Set<String> organizationIds = new HashSet<>();

    // Pour Active Directory
    private boolean isAdUser;
    private String adUsername;
}