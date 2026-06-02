package com.easybulk.smsgatewayservice.controller;

import com.easybulk.common.dto.ApiResponse;
import com.easybulk.smsgatewayservice.model.Connector;
import com.easybulk.smsgatewayservice.service.ConnectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connectors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConnectorController {

    private final ConnectorService connectorService;

    @PostMapping
    public ResponseEntity<ApiResponse<Connector>> createConnector(@RequestBody Connector connector) {
        Connector created = connectorService.createConnector(connector);
        return ResponseEntity.ok(ApiResponse.success("Connector created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Connector>>> getAllConnectors() {
        List<Connector> connectors = connectorService.getAllConnectors();
        return ResponseEntity.ok(ApiResponse.success(connectors));
    }

    @GetMapping("/{connectorId}")
    public ResponseEntity<ApiResponse<Connector>> getConnector(@PathVariable String connectorId) {
        Connector connector = connectorService.getConnectorById(connectorId);
        return ResponseEntity.ok(ApiResponse.success(connector));
    }

    @PostMapping("/{connectorId}/check-availability")
    public ResponseEntity<ApiResponse<Void>> checkAvailability(@PathVariable String connectorId) {
        connectorService.checkConnectorAvailability(connectorId);
        return ResponseEntity.ok(ApiResponse.success("Availability checked", null));
    }

    @PutMapping("/{connectorId}")
    public ResponseEntity<ApiResponse<Connector>> updateConnector(
            @PathVariable String connectorId,
            @RequestBody Connector connector) {

        Connector updated = connectorService.updateConnector(connectorId, connector);
        return ResponseEntity.ok(ApiResponse.success("Connector updated successfully", updated));
    }

    @DeleteMapping("/{connectorId}")
    public ResponseEntity<ApiResponse<Void>> deleteConnector(@PathVariable String connectorId) {
        connectorService.deleteConnector(connectorId);
        return ResponseEntity.ok(ApiResponse.success("Connector deleted successfully", null));
    }
}