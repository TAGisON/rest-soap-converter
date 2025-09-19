package com.example.restsoapconverter.entity;

import javax.persistence.*;

/**
 * Entity representing mapping configuration for converting REST responses to SOAP.
 */
@Entity
@Table(name = "mappings")
public class Mapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private SoapEndpoint endpoint;

    @Column(name = "mapping_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MappingType mappingType;

    @Column(name = "mapping_definition", columnDefinition = "TEXT")
    private String mappingDefinition;

    @Column(name = "mapper_class")
    private String mapperClass;

    @Column(name = "execution_order")
    private Integer executionOrder = 1;

    // Enum for mapping types
    public enum MappingType {
        MAPSTRUCT, JOLT, GROOVY
    }

    // Constructors
    public Mapping() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SoapEndpoint getEndpoint() { return endpoint; }
    public void setEndpoint(SoapEndpoint endpoint) { this.endpoint = endpoint; }

    public MappingType getMappingType() { return mappingType; }
    public void setMappingType(MappingType mappingType) { this.mappingType = mappingType; }

    public String getMappingDefinition() { return mappingDefinition; }
    public void setMappingDefinition(String mappingDefinition) { this.mappingDefinition = mappingDefinition; }

    public String getMapperClass() { return mapperClass; }
    public void setMapperClass(String mapperClass) { this.mapperClass = mapperClass; }

    public Integer getExecutionOrder() { return executionOrder; }
    public void setExecutionOrder(Integer executionOrder) { this.executionOrder = executionOrder; }
}
