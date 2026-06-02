package com.easybulk.contactservice.model;

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
@Document(collection = "contacts")
public class Contact {

    @Id
    private String id;

    @Indexed
    private String userId; // Cloisonnement par utilisateur

    @Indexed
    private String phone; // Format international E.164

    private String firstName;

    private String lastName;

    @Indexed
    private String email;

    private String country;

    private Set<String> tags = new HashSet<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}