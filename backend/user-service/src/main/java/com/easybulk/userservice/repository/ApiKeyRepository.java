package com.easybulk.userservice.repository;

import com.easybulk.userservice.Model.ApiKey;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends MongoRepository<ApiKey, String> {
    List<ApiKey> findByUserId(String userId);
    Optional<ApiKey> findByKeyHash(String keyHash);
}
