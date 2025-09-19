package com.example.restsoapconverter.dto;

import com.example.restsoapconverter.entity.RestCall;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class RestCallConfig {

    @NotNull(message = "Sequence order is required")
    private Integer sequenceOrder = 1;

    @NotNull(message = "HTTP method is required")
    private RestCall.HttpMethod method = RestCall.HttpMethod.GET;

    @NotBlank(message = "URL template is required")
    private String urlTemplate;

    private RestCall.AuthType authType = RestCall.AuthType.NONE;
    private Integer timeoutMs = 30000;
    private Boolean parallelExecution = false;

    // Constructors
    public RestCallConfig() {}

    // Getters and Setters
    public Integer getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }

    public RestCall.HttpMethod getMethod() { return method; }
    public void setMethod(RestCall.HttpMethod method) { this.method = method; }

    public String getUrlTemplate() { return urlTemplate; }
    public void setUrlTemplate(String urlTemplate) { this.urlTemplate = urlTemplate; }

    public RestCall.AuthType getAuthType() { return authType; }
    public void setAuthType(RestCall.AuthType authType) { this.authType = authType; }

    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }

    public Boolean getParallelExecution() { return parallelExecution; }
    public void setParallelExecution(Boolean parallelExecution) { this.parallelExecution = parallelExecution; }
}
