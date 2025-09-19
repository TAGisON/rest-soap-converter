package com.example.restsoapconverter.entity;

import javax.persistence.*;

/**
 * Entity representing error mapping configuration.
 */
@Entity
@Table(name = "error_mappings")
public class ErrorMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private SoapEndpoint endpoint;

    @Column(name = "rest_status_code")
    private Integer restStatusCode;

    @Column(name = "soap_fault_code", nullable = false)
    private String soapFaultCode;

    @Column(name = "soap_fault_message", nullable = false)
    private String soapFaultMessage;

    // Constructors, getters, and setters
    public ErrorMapping() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SoapEndpoint getEndpoint() { return endpoint; }
    public void setEndpoint(SoapEndpoint endpoint) { this.endpoint = endpoint; }

    public Integer getRestStatusCode() { return restStatusCode; }
    public void setRestStatusCode(Integer restStatusCode) { this.restStatusCode = restStatusCode; }

    public String getSoapFaultCode() { return soapFaultCode; }
    public void setSoapFaultCode(String soapFaultCode) { this.soapFaultCode = soapFaultCode; }

    public String getSoapFaultMessage() { return soapFaultMessage; }
    public void setSoapFaultMessage(String soapFaultMessage) { this.soapFaultMessage = soapFaultMessage; }
}
