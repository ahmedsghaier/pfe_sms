package com.easybulk.engagementpredictionservice.service;

import com.easybulk.engagementpredictionservice.dto.CampaignPredictionRequest;
import com.easybulk.engagementpredictionservice.dto.CampaignPredictionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

@Service
public class PythonModelBridge {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CampaignPredictionResponse predict(CampaignPredictionRequest request) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python3", "src/main/resources/predict.py");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Envoi des données en JSON vers Python
            try (PrintWriter writer = new PrintWriter(process.getOutputStream(), true)) {
                writer.println(objectMapper.writeValueAsString(request));
            }

            // Lecture de la réponse
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python script exited with code " + exitCode);
            }

            return objectMapper.readValue(output.toString(), CampaignPredictionResponse.class);

        } catch (Exception e) {
            e.printStackTrace();
            return getFallbackResponse();
        }
    }

    private CampaignPredictionResponse getFallbackResponse() {
        return new CampaignPredictionResponse(
                0.285,
                "09h-12h",
                "matin",
                0.65,
                List.of(),
                "Recommandation basée sur les moyennes historiques (fallback)",
                285
        );
    }
}
