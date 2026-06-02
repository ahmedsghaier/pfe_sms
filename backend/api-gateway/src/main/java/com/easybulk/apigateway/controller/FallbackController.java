package com.easybulk.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        return createFallbackResponse("Auth service is currently unavailable");
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> userFallback() {
        return createFallbackResponse("User service is currently unavailable");
    }

    @GetMapping("/campaign")
    public ResponseEntity<Map<String, Object>> campaignFallback() {
        return createFallbackResponse("Campaign service is currently unavailable");
    }

    @GetMapping("/contact")
    public ResponseEntity<Map<String, Object>> contactFallback() {
        return createFallbackResponse("Contact service is currently unavailable");
    }

    @GetMapping("/sms")
    public ResponseEntity<Map<String, Object>> smsFallback() {
        return createFallbackResponse("SMS Gateway service is currently unavailable");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("error", "SERVICE_UNAVAILABLE");

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}