package com.easybulk.contactservice.service;


import com.easybulk.contactservice.dto.CreateContactRequest;
import com.easybulk.contactservice.dto.ImportResult;
import com.easybulk.contactservice.model.Contact;
import com.easybulk.contactservice.repository.ContactRepository;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public Contact createContact(String userId, CreateContactRequest request) {
        log.info("Creating contact for user: {}", userId);

        String normalizedPhone = normalizePhoneNumber(request.getPhone());

        // Vérifier doublons
        if (contactRepository.existsByUserIdAndPhone(userId, normalizedPhone)) {
            throw new RuntimeException("Contact already exists with this phone number");
        }

        Contact contact = Contact.builder()
                .userId(userId)
                .phone(normalizedPhone)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .country(getCountryFromPhone(normalizedPhone))
                .tags(request.getTags() != null ? request.getTags() : new HashSet<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return contactRepository.save(contact);
    }

    public Page<Contact> getUserContacts(String userId, Pageable pageable) {
        return contactRepository.findByUserId(userId, pageable);
    }

    public Page<Contact> searchContacts(String userId, String searchTerm, Pageable pageable) {
        return contactRepository.findByUserIdAndPhoneContainingOrFirstNameContainingOrLastNameContaining(
                userId, searchTerm, searchTerm, searchTerm, pageable);
    }

    public List<Contact> getContactsByTags(String userId, Set<String> tags) {
        return contactRepository.findByUserIdAndTagsIn(userId, tags);
    }

    public void addTagsToContacts(String userId, List<String> contactIds, Set<String> tags) {
        if (contactIds.size() > 3000) {
            throw new RuntimeException("Cannot process more than 3000 contacts at once");
        }

        List<Contact> contacts = contactRepository.findByUserIdAndIdIn(userId, contactIds);
        contacts.forEach(contact -> {
            contact.getTags().addAll(tags);
            contact.setUpdatedAt(LocalDateTime.now());
        });

        contactRepository.saveAll(contacts);
        log.info("Added tags {} to {} contacts", tags, contacts.size());
    }

    public void removeTagsFromContacts(String userId, List<String> contactIds, Set<String> tags) {
        if (contactIds.size() > 3000) {
            throw new RuntimeException("Cannot process more than 3000 contacts at once");
        }

        List<Contact> contacts = contactRepository.findByUserIdAndIdIn(userId, contactIds);
        contacts.forEach(contact -> {
            contact.getTags().removeAll(tags);
            contact.setUpdatedAt(LocalDateTime.now());
        });

        contactRepository.saveAll(contacts);
    }

    public void deleteContacts(String userId, List<String> contactIds) {
        if (contactIds.size() > 3000) {
            throw new RuntimeException("Cannot process more than 3000 contacts at once");
        }

        contactRepository.deleteByUserIdAndIdIn(userId, contactIds);
        log.info("Deleted {} contacts for user {}", contactIds.size(), userId);
    }

    public ImportResult importFromCsv(String userId, MultipartFile file) throws IOException {
        log.info("Importing contacts from CSV for user: {}", userId);

        ImportResult result = ImportResult.builder()
                .totalRows(0)
                .successCount(0)
                .duplicateCount(0)
                .errorCount(0)
                .build();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();

            // Skip header
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                result.setTotalRows(result.getTotalRows() + 1);

                if (row.length < 1) {
                    result.setErrorCount(result.getErrorCount() + 1);
                    result.getErrors().add(ImportResult.ImportError.builder()
                            .row(i + 1)
                            .reason("Empty row")
                            .build());
                    continue;
                }

                String phone = row[0].trim();
                String firstName = row.length > 1 ? row[1].trim() : null;
                String lastName = row.length > 2 ? row[2].trim() : null;
                String email = row.length > 3 ? row[3].trim() : null;

                try {
                    String normalizedPhone = normalizePhoneNumber(phone);

                    if (contactRepository.existsByUserIdAndPhone(userId, normalizedPhone)) {
                        result.setDuplicateCount(result.getDuplicateCount() + 1);
                        continue;
                    }

                    Contact contact = Contact.builder()
                            .userId(userId)
                            .phone(normalizedPhone)
                            .firstName(firstName)
                            .lastName(lastName)
                            .email(email)
                            .country(getCountryFromPhone(normalizedPhone))
                            .tags(new HashSet<>())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    contactRepository.save(contact);
                    result.setSuccessCount(result.getSuccessCount() + 1);

                } catch (Exception e) {
                    result.setErrorCount(result.getErrorCount() + 1);
                    result.getErrors().add(ImportResult.ImportError.builder()
                            .row(i + 1)
                            .phone(phone)
                            .reason(e.getMessage())
                            .build());
                }
            }

        } catch (CsvException e) {
            throw new RuntimeException("Error reading CSV file", e);
        }

        log.info("Import completed: {} success, {} duplicates, {} errors",
                result.getSuccessCount(), result.getDuplicateCount(), result.getErrorCount());

        return result;
    }

    private String normalizePhoneNumber(String phone) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phone, null);

            if (!phoneNumberUtil.isValidNumber(number)) {
                throw new RuntimeException("Invalid phone number");
            }

            // Format E.164
            return phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);

        } catch (NumberParseException e) {
            throw new RuntimeException("Invalid phone number format: " + phone);
        }
    }

    private String getCountryFromPhone(String phone) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phone, null);
            return phoneNumberUtil.getRegionCodeForNumber(number);
        } catch (NumberParseException e) {
            return "UNKNOWN";
        }
    }
}
