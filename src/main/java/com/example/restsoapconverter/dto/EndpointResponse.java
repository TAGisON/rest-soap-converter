package com.example.restsoapconverter.dto;

import java.time.LocalDateTime;
import java.util.List;

public class EndpointResponse {

    private Long id;
    private String name;
    private String operationName;
    private String namespace;
    private String wsdlPath;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RestCallConfig> restCalls;
    private List<MappingConfig> mappings;

    // Constructors
    public EndpointResponse() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOperationName() { return operationName; }
    public void setOperationName(String operationName) { this.operationName = operationName; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public String getWsdlPath() { return wsdlPath; }
    public void setWsdlPath(String wsdlPath) { this.wsdlPath = wsdlPath; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<RestCallConfig> getRestCalls() { return restCalls; }
    public void setRestCalls(List<RestCallConfig> restCalls) { this.restCalls = restCalls; }

    public List<MappingConfig> getMappings() { return mappings; }
    public void setMappings(List<MappingConfig> mappings) { this.mappings = mappings; }
}
