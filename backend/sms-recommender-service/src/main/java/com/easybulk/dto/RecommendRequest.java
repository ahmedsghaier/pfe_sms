package com.easybulk.dto;

import lombok.Data;

@Data
public class RecommendRequest {
    private String partialText;
    private int topK = 5;
}
