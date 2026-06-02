package com.easybulk.userservice.repository;

import com.easybulk.userservice.Model.UserInvitation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserInvitationRepository extends MongoRepository<UserInvitation, String> {
    Optional<UserInvitation> findByEmail(String email);
    Optional<UserInvitation> findByVerificationToken(String token);
}
