package com.easybulk.smsgatewayservice.service;


import com.easybulk.smsgatewayservice.model.Route;
import com.easybulk.smsgatewayservice.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RouteRepository routeRepository;

    public Route createRoute(Route route) {
        // Vérifier que la priorité est dans la plage valide
        if (route.getPriority() < 1 || route.getPriority() > 199) {
            throw new RuntimeException("Priority must be between 1 and 199");
        }

        // Vérifier unicité de la priorité
        if (routeRepository.existsByPriority(route.getPriority())) {
            throw new RuntimeException("A route with this priority already exists");
        }

        route.setActive(true);
        route.setDefault(false);
        route.setCreatedAt(LocalDateTime.now());
        route.setUpdatedAt(LocalDateTime.now());

        return routeRepository.save(route);
    }

    public Route createDefaultRoute() {
        Route defaultRoute = Route.builder()
                .groupId("ALL")
                .alphaHeader("*")
                .destinationPattern(".*")
                .priority(200)
                .active(true)
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return routeRepository.save(defaultRoute);
    }

    public List<Route> getAllRoutes() {
        return routeRepository.findAllByOrderByPriorityAsc();
    }

    public Route findBestRoute(String groupId, String alphaHeader, String destinationNumber) {
        List<Route> routes = routeRepository.findByActiveTrue();

        return routes.stream()
                .filter(route -> matchesRoute(route, groupId, alphaHeader, destinationNumber))
                .min(Comparator.comparingInt(Route::getPriority))
                .orElseGet(() -> routeRepository.findByIsDefaultTrue()
                        .orElseThrow(() -> new RuntimeException("No default route found")));
    }

    private boolean matchesRoute(Route route, String groupId, String alphaHeader, String destinationNumber) {
        // Vérifier groupe
        if (!route.getGroupId().equals("ALL") && !route.getGroupId().equals(groupId)) {
            return false;
        }

        // Vérifier alpha header
        if (!route.getAlphaHeader().equals("*") && !route.getAlphaHeader().equals(alphaHeader)) {
            return false;
        }

        // Vérifier pattern destination
        if (route.getDestinationPattern() != null) {
            Pattern pattern = Pattern.compile(route.getDestinationPattern());
            if (!pattern.matcher(destinationNumber).matches()) {
                return false;
            }
        }

        return true;
    }

    public void deleteRoute(String routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found"));

        if (route.isDefault()) {
            throw new RuntimeException("Cannot delete default route");
        }

        routeRepository.deleteById(routeId);
        log.info("Deleted route: {}", routeId);
    }
}