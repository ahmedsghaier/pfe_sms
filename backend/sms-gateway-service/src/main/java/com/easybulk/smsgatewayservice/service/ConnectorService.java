package com.easybulk.smsgatewayservice.service;

import com.easybulk.smsgatewayservice.model.Connector;
import com.easybulk.smsgatewayservice.repository.ConnectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectorService {

    private final ConnectorRepository connectorRepository;

    public Connector createConnector(Connector connector) {
        connector.setActive(true);
        connector.setAvailable(true);
        connector.setCreatedAt(LocalDateTime.now());
        connector.setUpdatedAt(LocalDateTime.now());

        log.info("Creating connector: {} of type {}", connector.getName(), connector.getType());

        return connectorRepository.save(connector);
    }

    public List<Connector> getAllConnectors() {
        return connectorRepository.findAll();
    }

    public List<Connector> getActiveConnectors() {
        return connectorRepository.findByActiveTrue();
    }

    public Connector getConnectorById(String connectorId) {
        return connectorRepository.findById(connectorId)
                .orElseThrow(() -> new RuntimeException("Connector not found"));
    }

    public void checkConnectorAvailability(String connectorId) {
        Connector connector = getConnectorById(connectorId);

        // Simulation de vérification
        boolean isAvailable = pingConnector(connector);

        if (!isAvailable && connector.isAvailable()) {
            connector.setUnavailableDurationMinutes(0L);
        } else if (!isAvailable) {
            long duration = connector.getUnavailableDurationMinutes() != null
                    ? connector.getUnavailableDurationMinutes() + 5
                    : 5;
            connector.setUnavailableDurationMinutes(duration);
        }

        connector.setAvailable(isAvailable);
        connector.setLastAvailabilityCheck(LocalDateTime.now());

        connectorRepository.save(connector);

        log.info("Connector {} availability: {}", connectorId, isAvailable);
    }

    private boolean pingConnector(Connector connector) {
        // TODO: Implémenter le ping réel selon le type de connecteur
        return true;
    }

    public Connector updateConnector(String connectorId, Connector updates) {
        Connector connector = getConnectorById(connectorId);

        connector.setName(updates.getName());
        connector.setNativeConfig(updates.getNativeConfig());
        connector.setLoadBalancerConfig(updates.getLoadBalancerConfig());
        connector.setFailoverConfig(updates.getFailoverConfig());
        connector.setUpdatedAt(LocalDateTime.now());

        return connectorRepository.save(connector);
    }

    public void deleteConnector(String connectorId) {
        Connector connector = getConnectorById(connectorId);

        if (connector.getType() == Connector.ConnectorType.NATIVE
                && "auto-created".equals(connector.getName())) {
            throw new RuntimeException("Cannot delete auto-created native connector");
        }

        connectorRepository.deleteById(connectorId);
        log.info("Deleted connector: {}", connectorId);
    }
}