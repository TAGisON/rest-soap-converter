package com.example.restsoapconverter.controller;

import com.example.restsoapconverter.dto.EndpointCreateRequest;
import com.example.restsoapconverter.dto.EndpointResponse;
import com.example.restsoapconverter.service.EndpointManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin REST API controller for managing SOAP endpoints.
 */
@RestController
@RequestMapping("/admin/endpoints")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private EndpointManagerService endpointManagerService;

    @PostMapping
    public ResponseEntity<EndpointResponse> createEndpoint(@Valid @RequestBody EndpointCreateRequest request) {
        String correlationId = UUID.randomUUID().toString();
        logger.info("Creating endpoint: name={}, correlationId={}", request.getName(), correlationId);

        try {
            EndpointResponse response = endpointManagerService.createEndpoint(request, correlationId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Failed to create endpoint: correlationId={}", correlationId, e);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<List<EndpointResponse>> getAllEndpoints() {
        List<EndpointResponse> endpoints = endpointManagerService.getAllEndpoints();
        return ResponseEntity.ok(endpoints);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EndpointResponse> getEndpoint(@PathVariable Long id) {
        EndpointResponse response = endpointManagerService.getEndpoint(id);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<EndpointResponse> updateEndpoint(@PathVariable Long id, 
                                                          @Valid @RequestBody EndpointCreateRequest request) {
        String correlationId = UUID.randomUUID().toString();
        EndpointResponse response = endpointManagerService.updateEndpoint(id, request, correlationId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable Long id) {
        String correlationId = UUID.randomUUID().toString();
        boolean deleted = endpointManagerService.deleteEndpoint(id, correlationId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testEndpoint(@PathVariable Long id, 
                                                           @RequestBody(required = false) String sampleRequest) {
        String correlationId = UUID.randomUUID().toString();
        try {
            Map<String, Object> testResult = endpointManagerService.testEndpoint(id, sampleRequest, correlationId);
            return ResponseEntity.ok(testResult);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getEndpointsHealth() {
        Map<String, Object> healthStatus = endpointManagerService.getHealthStatus();
        return ResponseEntity.ok(healthStatus);
    }
}
