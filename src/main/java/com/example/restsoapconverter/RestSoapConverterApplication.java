package com.example.restsoapconverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main Spring Boot application class for Dynamic REST to SOAP Converter.
 * 
 * This application provides:
 * - Dynamic SOAP endpoints based on database configuration
 * - REST API calls to upstream services with authentication support
 * - Configurable mapping using MapStruct, JOLT, and Groovy
 * - Resilience patterns (retry, circuit breaker)
 * - Caching capabilities
 */
@SpringBootApplication
@EnableCaching
public class RestSoapConverterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestSoapConverterApplication.class, args);
    }
}
