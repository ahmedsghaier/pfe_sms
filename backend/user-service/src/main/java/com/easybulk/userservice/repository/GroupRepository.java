package com.easybulk.userservice.repository;

import com.easybulk.userservice.Model.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends MongoRepository<Group, String> {
    List<Group> findByOrganizationId(String organizationId);
    List<Group> findByOrganizationIdAndActive(String organizationId, boolean active);
}
