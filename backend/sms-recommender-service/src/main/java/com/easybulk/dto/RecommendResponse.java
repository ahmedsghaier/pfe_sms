package com.easybulk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)   // ✅ ajouté
public class RecommendResponse {
    private String mode;      // "autocomplete" | "next_word"
    private String lang;
    private List<Suggestion> suggestions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String word;
        private double score;
    }
}