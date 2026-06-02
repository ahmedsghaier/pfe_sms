package com.easybulk.contactservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class CreateContactRequest {

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String firstName;

    private String lastName;

    private String email;

    private Set<String> tags;
}