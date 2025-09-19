package com.example.restsoapconverter.entity;

import javax.persistence.*;

/**
 * Entity representing cache policy configuration for SOAP endpoints.
 */
@Entity
@Table(name = "cache_policies")
public class CachePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private SoapEndpoint endpoint;

    @Column(nullable = false)
    private Boolean enabled = false;

    @Column(name = "ttl_seconds")
    private Integer ttlSeconds = 300;

    // Constructors, getters, and setters
    public CachePolicy() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SoapEndpoint getEndpoint() { return endpoint; }
    public void setEndpoint(SoapEndpoint endpoint) { this.endpoint = endpoint; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Integer getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(Integer ttlSeconds) { this.ttlSeconds = ttlSeconds; }
}
