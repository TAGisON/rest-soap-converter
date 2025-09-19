package com.example.restsoapconverter.dto;

import com.example.restsoapconverter.entity.Mapping;

import javax.validation.constraints.NotNull;

public class MappingConfig {

    @NotNull(message = "Mapping type is required")
    private Mapping.MappingType mappingType;

    private String mappingDefinition;
    private String mapperClass;
    private Integer executionOrder = 1;

    // Constructors
    public MappingConfig() {}

    // Getters and Setters
    public Mapping.MappingType getMappingType() { return mappingType; }
    public void setMappingType(Mapping.MappingType mappingType) { this.mappingType = mappingType; }

    public String getMappingDefinition() { return mappingDefinition; }
    public void setMappingDefinition(String mappingDefinition) { this.mappingDefinition = mappingDefinition; }

    public String getMapperClass() { return mapperClass; }
    public void setMapperClass(String mapperClass) { this.mapperClass = mapperClass; }

    public Integer getExecutionOrder() { return executionOrder; }
    public void setExecutionOrder(Integer executionOrder) { this.executionOrder = executionOrder; }
}
