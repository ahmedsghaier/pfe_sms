package com.easybulk.engagementpredictionservice.service;

import com.easybulk.engagementpredictionservice.dto.CampaignPredictionRequest;
import com.easybulk.engagementpredictionservice.dto.CampaignPredictionResponse;
import org.springframework.stereotype.Service;

@Service
public class EngagementPredictionService {

    private final PythonModelBridge pythonModelBridge;

    public EngagementPredictionService(PythonModelBridge pythonModelBridge) {
        this.pythonModelBridge = pythonModelBridge;
    }

    public CampaignPredictionResponse predictEngagement(CampaignPredictionRequest request) {
        return pythonModelBridge.predict(request);
    }
}
