package com.easybulk.authservice.Repository;

import com.easybulk.authservice.model.UserOrganizationRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserOrganizationRoleRepository extends MongoRepository<UserOrganizationRole, String> {
    List<UserOrganizationRole> findByUserId(String userId);
    List<UserOrganizationRole> findByUserIdAndOrganizationId(String userId, String organizationId);
}
