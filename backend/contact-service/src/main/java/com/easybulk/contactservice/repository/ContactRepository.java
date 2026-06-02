package com.easybulk.contactservice.repository;


import com.easybulk.contactservice.model.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ContactRepository extends MongoRepository<Contact, String> {

    Page<Contact> findByUserId(String userId, Pageable pageable);

    boolean existsByUserIdAndPhone(String userId, String phone);

    List<Contact> findByUserIdAndIdIn(String userId, List<String> ids);

    List<Contact> findByUserIdAndTagsIn(String userId, Set<String> tags);

    Page<Contact> findByUserIdAndPhoneContainingOrFirstNameContainingOrLastNameContaining(
            String userId, String phone, String firstName, String lastName, Pageable pageable);

    void deleteByUserIdAndIdIn(String userId, List<String> ids);
}
