package com.easybulk.contactservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    private int totalRows;

    private int successCount;

    private int duplicateCount;

    private int errorCount;

    @Builder.Default
    private List<ImportError> errors = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportError {
        private int row;
        private String phone;
        private String reason;
    }
}