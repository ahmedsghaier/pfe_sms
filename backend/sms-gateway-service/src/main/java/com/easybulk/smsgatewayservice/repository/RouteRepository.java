package com.easybulk.smsgatewayservice.repository;


import com.easybulk.smsgatewayservice.model.Route;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends MongoRepository<Route, String> {
    List<Route> findByActiveTrue();
    List<Route> findAllByOrderByPriorityAsc();
    boolean existsByPriority(int priority);
    Optional<Route> findByIsDefaultTrue();
}