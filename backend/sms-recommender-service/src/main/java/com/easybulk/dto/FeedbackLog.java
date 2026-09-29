package com.easybulk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "sms_feedback_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackLog {

    @Id
    private String id;

    /**
     * Texte saisi par l'utilisateur au moment de la suggestion
     * Exemple : "Bonj"
     */
    private String partialText;

    /**
     * Suggestion sélectionnée
     * Exemple : "Bonjour"
     */
    private String selectedWord;

    /**
     * Utilisateur ayant effectué la sélection
     */
    private String userId;

    /**
     * Date de création du feedback
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}