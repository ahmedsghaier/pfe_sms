package com.easybulk.smsgatewayservice.repository;


import com.easybulk.smsgatewayservice.model.Connector;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnectorRepository extends MongoRepository<Connector, String> {
    List<Connector> findByActiveTrue();
    List<Connector> findByType(Connector.ConnectorType type);
}