package com.easybulk.contactservice.controller;

import com.easybulk.common.dto.ApiResponse;
import com.easybulk.contactservice.dto.CreateContactRequest;
import com.easybulk.contactservice.dto.ImportResult;
import com.easybulk.contactservice.model.Contact;
import com.easybulk.contactservice.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<ApiResponse<Contact>> createContact(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateContactRequest request) {

        Contact contact = contactService.createContact(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Contact created successfully", contact));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Contact>>> getContacts(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Contact> contacts = contactService.getUserContacts(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<Contact>>> searchContacts(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Contact> contacts = contactService.searchContacts(userId, query, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<ImportResult>> importContacts(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.getSize() > 30 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File size exceeds 30MB limit"));
        }

        ImportResult result = contactService.importFromCsv(userId, file);
        return ResponseEntity.ok(ApiResponse.success("Import completed", result));
    }

    @PostMapping("/bulk/add-tags")
    public ResponseEntity<ApiResponse<Void>> addTagsToContacts(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody BulkTagRequest request) {

        contactService.addTagsToContacts(userId, request.getContactIds(), request.getTags());
        return ResponseEntity.ok(ApiResponse.success("Tags added successfully", null));
    }

    @PostMapping("/bulk/remove-tags")
    public ResponseEntity<ApiResponse<Void>> removeTagsFromContacts(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody BulkTagRequest request) {

        contactService.removeTagsFromContacts(userId, request.getContactIds(), request.getTags());
        return ResponseEntity.ok(ApiResponse.success("Tags removed successfully", null));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> deleteContacts(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody List<String> contactIds) {

        contactService.deleteContacts(userId, contactIds);
        return ResponseEntity.ok(ApiResponse.success("Contacts deleted successfully", null));
    }

    @lombok.Data
    static class BulkTagRequest {
        private List<String> contactIds;
        private Set<String> tags;
    }
}
