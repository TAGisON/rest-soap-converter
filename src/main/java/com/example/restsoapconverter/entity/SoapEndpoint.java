package com.example.restsoapconverter.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a SOAP endpoint configuration.
 * Each endpoint defines how to convert REST responses to SOAP responses.
 */
@Entity
@Table(name = "soap_endpoints")
public class SoapEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "operation_name", nullable = false)
    private String operationName;

    @Column(nullable = false)
    private String namespace;

    @Column(name = "wsdl_path")
    private String wsdlPath;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "endpoint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RestCall> restCalls;

    @OneToMany(mappedBy = "endpoint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Mapping> mappings;

    @OneToMany(mappedBy = "endpoint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ErrorMapping> errorMappings;

    @OneToOne(mappedBy = "endpoint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CachePolicy cachePolicy;

    // Constructors
    public SoapEndpoint() {}

    public SoapEndpoint(String name, String operationName, String namespace) {
        this.name = name;
        this.operationName = operationName;
        this.namespace = namespace;
    }

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

    public List<RestCall> getRestCalls() { return restCalls; }
    public void setRestCalls(List<RestCall> restCalls) { this.restCalls = restCalls; }

    public List<Mapping> getMappings() { return mappings; }
    public void setMappings(List<Mapping> mappings) { this.mappings = mappings; }

    public List<ErrorMapping> getErrorMappings() { return errorMappings; }
    public void setErrorMappings(List<ErrorMapping> errorMappings) { this.errorMappings = errorMappings; }

    public CachePolicy getCachePolicy() { return cachePolicy; }
    public void setCachePolicy(CachePolicy cachePolicy) { this.cachePolicy = cachePolicy; }
}
