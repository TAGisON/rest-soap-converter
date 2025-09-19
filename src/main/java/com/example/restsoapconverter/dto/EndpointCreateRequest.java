package com.example.restsoapconverter.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

public class EndpointCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Operation name is required")
    private String operationName;

    @NotBlank(message = "Namespace is required")
    private String namespace;

    private String wsdlPath;
    private Boolean enabled = true;

    @NotNull(message = "At least one REST call is required")
    @Valid
    private List<RestCallConfig> restCalls;

    @Valid
    private List<MappingConfig> mappings;

    // Constructors
    public EndpointCreateRequest() {}

    // Getters and Setters
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

    public List<RestCallConfig> getRestCalls() { return restCalls; }
    public void setRestCalls(List<RestCallConfig> restCalls) { this.restCalls = restCalls; }

    public List<MappingConfig> getMappings() { return mappings; }
    public void setMappings(List<MappingConfig> mappings) { this.mappings = mappings; }
}
