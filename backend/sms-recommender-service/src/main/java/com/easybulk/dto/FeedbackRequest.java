package com.easybulk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    private String partialText;
    private String selectedWord;
    private String userId;       // pour personnalisation future
}