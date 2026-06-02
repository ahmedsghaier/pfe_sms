package com.easybulk.campaignservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sms_logs")
public class SmsLog {

    @Id
    private String id;

    @Indexed
    private String campaignId;

    private String contactId;

    private String phoneNumber;

    private String message;

    private SmsStatus status;

    private String statusCode;

    private String errorMessage;

    private String connectorId;

    private LocalDateTime sentAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime createdAt;

    public enum SmsStatus {
        NOT_PROCESSED(0, "Pas traité"),
        IN_PROGRESS(1, "En cours"),
        DELIVERED(2, "Livré"),
        NOT_SENT(3, "Non envoyé"),
        SENDING(4, "En cours d'envoi"),
        PENDING_OPERATOR(8, "À traiter opérateur"),
        FAILED(16, "Échec");

        private final int code;
        private final String label;

        SmsStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }
}
