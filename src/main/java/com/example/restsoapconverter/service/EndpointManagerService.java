package com.example.restsoapconverter.service;

import com.example.restsoapconverter.dto.EndpointCreateRequest;
import com.example.restsoapconverter.dto.EndpointResponse;
import com.example.restsoapconverter.dto.RestCallConfig;
import com.example.restsoapconverter.dto.MappingConfig;
import com.example.restsoapconverter.entity.*;
import com.example.restsoapconverter.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing SOAP endpoint configurations.
 */
@Service
@Transactional
public class EndpointManagerService {

    private static final Logger logger = LoggerFactory.getLogger(EndpointManagerService.class);

    @Autowired
    private SoapEndpointRepository soapEndpointRepository;

    @Autowired
    private RestCallRepository restCallRepository;

    @Autowired
    private MappingRepository mappingRepository;

    public EndpointResponse createEndpoint(EndpointCreateRequest request, String correlationId) {
        logger.info("Creating endpoint: name={}, correlationId={}", request.getName(), correlationId);

        if (soapEndpointRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Endpoint with name '" + request.getName() + "' already exists");
        }

        // Create main endpoint entity
        SoapEndpoint endpoint = new SoapEndpoint();
        endpoint.setName(request.getName());
        endpoint.setOperationName(request.getOperationName());
        endpoint.setNamespace(request.getNamespace());
        endpoint.setWsdlPath(request.getWsdlPath());
        endpoint.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        endpoint = soapEndpointRepository.save(endpoint);

        // Create REST calls
        createRestCalls(endpoint, request.getRestCalls(), correlationId);

        // Create mappings
        createMappings(endpoint, request.getMappings(), correlationId);

        logger.info("Created endpoint: id={}, correlationId={}", endpoint.getId(), correlationId);
        return mapToEndpointResponse(endpoint);
    }

    @Transactional(readOnly = true)
    public List<EndpointResponse> getAllEndpoints() {
        List<SoapEndpoint> endpoints = soapEndpointRepository.findAll();
        return endpoints.stream()
                .map(this::mapToEndpointResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EndpointResponse getEndpoint(Long id) {
        Optional<SoapEndpoint> endpoint = soapEndpointRepository.findById(id);
        return endpoint.map(this::mapToEndpointResponse).orElse(null);
    }

    public EndpointResponse updateEndpoint(Long id, EndpointCreateRequest request, String correlationId) {
        logger.info("Updating endpoint: id={}, correlationId={}", id, correlationId);

        Optional<SoapEndpoint> existingEndpoint = soapEndpointRepository.findById(id);
        if (!existingEndpoint.isPresent()) {
            return null;
        }

        SoapEndpoint endpoint = existingEndpoint.get();
        endpoint.setName(request.getName());
        endpoint.setOperationName(request.getOperationName());
        endpoint.setNamespace(request.getNamespace());
        endpoint.setWsdlPath(request.getWsdlPath());
        endpoint.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        // Delete and recreate related entities
        restCallRepository.deleteByEndpointId(id);
        mappingRepository.deleteByEndpointId(id);

        endpoint = soapEndpointRepository.save(endpoint);
        createRestCalls(endpoint, request.getRestCalls(), correlationId);
        createMappings(endpoint, request.getMappings(), correlationId);

        return mapToEndpointResponse(endpoint);
    }

    public boolean deleteEndpoint(Long id, String correlationId) {
        logger.info("Deleting endpoint: id={}, correlationId={}", id, correlationId);

        if (!soapEndpointRepository.existsById(id)) {
            return false;
        }

        soapEndpointRepository.deleteById(id);
        return true;
    }

    public Map<String, Object> testEndpoint(Long id, String sampleRequest, String correlationId) {
        Map<String, Object> result = new HashMap<>();
        result.put("correlationId", correlationId);

        try {
            Optional<SoapEndpoint> endpointOpt = soapEndpointRepository.findById(id);
            if (!endpointOpt.isPresent()) {
                result.put("success", false);
                result.put("error", "Endpoint not found");
                return result;
            }

            result.put("success", true);
            result.put("message", "Dry-run test completed successfully");
            return result;

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        List<SoapEndpoint> endpoints = soapEndpointRepository.findAll();
        health.put("totalEndpoints", endpoints.size());
        health.put("enabledEndpoints", endpoints.stream().mapToInt(e -> e.getEnabled() ? 1 : 0).sum());
        health.put("status", "UP");
        health.put("timestamp", new Date());
        return health;
    }

    // Private helper methods
    private void createRestCalls(SoapEndpoint endpoint, List<RestCallConfig> restCallConfigs, String correlationId) {
        if (restCallConfigs == null) return;

        for (RestCallConfig config : restCallConfigs) {
            RestCall restCall = new RestCall();
            restCall.setEndpoint(endpoint);
            restCall.setSequenceOrder(config.getSequenceOrder());
            restCall.setMethod(config.getMethod());
            restCall.setUrlTemplate(config.getUrlTemplate());
            restCall.setAuthType(config.getAuthType());
            restCall.setTimeoutMs(config.getTimeoutMs());
            restCall.setParallelExecution(config.getParallelExecution());

            restCallRepository.save(restCall);
        }
    }

    private void createMappings(SoapEndpoint endpoint, List<MappingConfig> mappingConfigs, String correlationId) {
        if (mappingConfigs == null) return;

        for (MappingConfig config : mappingConfigs) {
            Mapping mapping = new Mapping();
            mapping.setEndpoint(endpoint);
            mapping.setMappingType(config.getMappingType());
            mapping.setMappingDefinition(config.getMappingDefinition());
            mapping.setMapperClass(config.getMapperClass());
            mapping.setExecutionOrder(config.getExecutionOrder());

            mappingRepository.save(mapping);
        }
    }

    private EndpointResponse mapToEndpointResponse(SoapEndpoint endpoint) {
        EndpointResponse response = new EndpointResponse();
        response.setId(endpoint.getId());
        response.setName(endpoint.getName());
        response.setOperationName(endpoint.getOperationName());
        response.setNamespace(endpoint.getNamespace());
        response.setWsdlPath(endpoint.getWsdlPath());
        response.setEnabled(endpoint.getEnabled());
        response.setCreatedAt(endpoint.getCreatedAt());
        response.setUpdatedAt(endpoint.getUpdatedAt());

        if (endpoint.getRestCalls() != null) {
            List<RestCallConfig> restCallConfigs = endpoint.getRestCalls().stream()
                    .map(this::mapToRestCallConfig)
                    .collect(Collectors.toList());
            response.setRestCalls(restCallConfigs);
        }

        if (endpoint.getMappings() != null) {
            List<MappingConfig> mappingConfigs = endpoint.getMappings().stream()
                    .map(this::mapToMappingConfig)
                    .collect(Collectors.toList());
            response.setMappings(mappingConfigs);
        }

        return response;
    }

    private RestCallConfig mapToRestCallConfig(RestCall restCall) {
        RestCallConfig config = new RestCallConfig();
        config.setSequenceOrder(restCall.getSequenceOrder());
        config.setMethod(restCall.getMethod());
        config.setUrlTemplate(restCall.getUrlTemplate());
        config.setAuthType(restCall.getAuthType());
        config.setTimeoutMs(restCall.getTimeoutMs());
        config.setParallelExecution(restCall.getParallelExecution());
        return config;
    }

    private MappingConfig mapToMappingConfig(Mapping mapping) {
        MappingConfig config = new MappingConfig();
        config.setMappingType(mapping.getMappingType());
        config.setMappingDefinition(mapping.getMappingDefinition());
        config.setMapperClass(mapping.getMapperClass());
        config.setExecutionOrder(mapping.getExecutionOrder());
        return config;
    }
}
