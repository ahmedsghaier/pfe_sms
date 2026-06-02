package com.easybulk.smsgatewayservice.controller;


import com.easybulk.common.dto.ApiResponse;
import com.easybulk.smsgatewayservice.model.Route;
import com.easybulk.smsgatewayservice.service.RoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RouteController {

    private final RoutingService routingService;

    @PostMapping
    public ResponseEntity<ApiResponse<Route>> createRoute(@RequestBody Route route) {
        Route created = routingService.createRoute(route);
        return ResponseEntity.ok(ApiResponse.success("Route created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Route>>> getAllRoutes() {
        List<Route> routes = routingService.getAllRoutes();
        return ResponseEntity.ok(ApiResponse.success(routes));
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable String routeId) {
        routingService.deleteRoute(routeId);
        return ResponseEntity.ok(ApiResponse.success("Route deleted successfully", null));
    }
}