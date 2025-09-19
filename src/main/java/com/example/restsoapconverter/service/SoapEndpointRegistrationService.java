package com.example.restsoapconverter.service;

import com.example.restsoapconverter.entity.SoapEndpoint;
import com.example.restsoapconverter.repository.SoapEndpointRepository;
import com.example.restsoapconverter.soap.DynamicSoapEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.ws.server.endpoint.mapping.PayloadRootAnnotationMethodEndpointMapping;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class SoapEndpointRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(SoapEndpointRegistrationService.class);

    @Autowired
    private SoapEndpointRepository soapEndpointRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DynamicSoapEndpoint dynamicSoapEndpoint;

    @PostConstruct
    public void registerExistingEndpoints() {
        logger.info("Registering existing SOAP endpoints from database...");

        List<SoapEndpoint> endpoints = soapEndpointRepository.findByEnabledTrue();
        for (SoapEndpoint endpoint : endpoints) {
            registerSoapEndpoint(endpoint);
        }

        logger.info("Registered {} SOAP endpoints", endpoints.size());
    }

    public void registerSoapEndpoint(SoapEndpoint endpoint) {
        logger.info("Registering SOAP endpoint: operationName={}, namespace={}", 
                   endpoint.getOperationName(), endpoint.getNamespace());

        // Register the endpoint with the dynamic handler
        dynamicSoapEndpoint.registerEndpoint(endpoint);

        logger.info("Successfully registered SOAP endpoint: {}", endpoint.getOperationName());
    }

    public void unregisterSoapEndpoint(SoapEndpoint endpoint) {
        logger.info("Unregistering SOAP endpoint: {}", endpoint.getOperationName());
        dynamicSoapEndpoint.unregisterEndpoint(endpoint);
    }
}