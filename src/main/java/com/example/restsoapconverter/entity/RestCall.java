package com.example.restsoapconverter.entity;

import javax.persistence.*;
import java.util.Map;

/**
 * Entity representing a REST call configuration for a SOAP endpoint.
 */
@Entity
@Table(name = "rest_calls")
public class RestCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private SoapEndpoint endpoint;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder = 1;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private HttpMethod method = HttpMethod.GET;

    @Column(name = "url_template", nullable = false)
    private String urlTemplate;

    @Column(name = "auth_type")
    @Enumerated(EnumType.STRING)
    private AuthType authType = AuthType.NONE;

    @Column(name = "timeout_ms")
    private Integer timeoutMs = 30000;

    @Column(name = "parallel_execution")
    private Boolean parallelExecution = false;

    // Enums
    public enum HttpMethod {
        GET, POST, PUT, PATCH, DELETE
    }

    public enum AuthType {
        NONE, BASIC, API_KEY, BEARER, OAUTH2_CLIENT_CREDENTIALS, MTLS
    }

    // Constructors
    public RestCall() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SoapEndpoint getEndpoint() { return endpoint; }
    public void setEndpoint(SoapEndpoint endpoint) { this.endpoint = endpoint; }

    public Integer getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }

    public HttpMethod getMethod() { return method; }
    public void setMethod(HttpMethod method) { this.method = method; }

    public String getUrlTemplate() { return urlTemplate; }
    public void setUrlTemplate(String urlTemplate) { this.urlTemplate = urlTemplate; }

    public AuthType getAuthType() { return authType; }
    public void setAuthType(AuthType authType) { this.authType = authType; }

    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }

    public Boolean getParallelExecution() { return parallelExecution; }
    public void setParallelExecution(Boolean parallelExecution) { this.parallelExecution = parallelExecution; }
}
