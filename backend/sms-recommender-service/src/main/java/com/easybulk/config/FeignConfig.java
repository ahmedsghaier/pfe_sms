package com.easybulk.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.naming.ServiceUnavailableException;

@Configuration
public class FeignConfig {
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == 503)
                return new ServiceUnavailableException("ML service down");
            return new RuntimeException("ML error: " + response.status());
        };
    }
}
