package com.easybulk.campaignservice.controller;

import com.easybulk.campaignservice.model.MessageTemplate;
import com.easybulk.campaignservice.repository.MessageTemplateRepository;
import com.easybulk.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MessageTemplateController {

    private final MessageTemplateRepository templateRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MessageTemplate>>> getTemplates(
            @RequestParam(required = false) String type,
            @RequestHeader(value = "X-Organization-Id", required = false) String organizationId) {

        List<MessageTemplate> templates;

        if (type != null && !type.isEmpty()) {
            try {
                MessageTemplate.TemplateType templateType = MessageTemplate.TemplateType.valueOf(type);
                templates = templateRepository.findByTypeAndActiveTrue(templateType);
            } catch (IllegalArgumentException e) {
                templates = templateRepository.findByActiveTrue();
            }
        } else {
            templates = templateRepository.findByActiveTrue();
        }

        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageTemplate>> getTemplate(@PathVariable String id) {
        return templateRepository.findById(id)
                .map(t -> ResponseEntity.ok(ApiResponse.success(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MessageTemplate>> createTemplate(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) String organizationId,
            @RequestBody MessageTemplate template) {

        template.setUserId(userId);
        template.setOrganizationId(organizationId);
        template.setActive(true);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        MessageTemplate saved = templateRepository.save(template);
        return ResponseEntity.ok(ApiResponse.success("Template created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageTemplate>> updateTemplate(
            @PathVariable String id,
            @RequestBody MessageTemplate template) {

        return templateRepository.findById(id).map(existing -> {
            existing.setName(template.getName());
            existing.setContent(template.getContent());
            existing.setType(template.getType());
            existing.setUpdatedAt(LocalDateTime.now());
            MessageTemplate saved = templateRepository.save(existing);
            return ResponseEntity.ok(ApiResponse.success("Template updated", saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable String id) {
        templateRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted", null));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<MessageTemplate>> toggleStatus(@PathVariable String id) {
        return templateRepository.findById(id).map(existing -> {
            existing.setActive(!existing.isActive());
            existing.setUpdatedAt(LocalDateTime.now());
            MessageTemplate saved = templateRepository.save(existing);
            return ResponseEntity.ok(ApiResponse.success("Status toggled", saved));
        }).orElse(ResponseEntity.notFound().build());
    }
}