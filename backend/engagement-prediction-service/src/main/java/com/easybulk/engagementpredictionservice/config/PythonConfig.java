package com.easybulk.engagementpredictionservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PythonConfig {

    @Value("${python.executable:python3}")
    private String pythonExecutable;

    @Value("${models.path:models}")
    private String modelsPath;

    public String getPythonExecutable() {
        return pythonExecutable;
    }

    public String getModelsPath() {
        return modelsPath;
    }
}
