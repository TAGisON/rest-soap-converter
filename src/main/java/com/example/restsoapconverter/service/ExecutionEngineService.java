// ENHANCED VERSION: src/main/java/com/example/restsoapconverter/service/ExecutionEngineService.java
package com.example.restsoapconverter.service;

import com.example.restsoapconverter.entity.SoapEndpoint;
import com.example.restsoapconverter.entity.RestCall;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.transaction.annotation.Transactional;
import com.example.restsoapconverter.repository.SoapEndpointRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class ExecutionEngineService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionEngineService.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SoapEndpointRepository soapEndpointRepository;

    public Map<String, Object> executeEndpoint(SoapEndpoint endpoint, Map<String, Object> parameters) throws Exception {
        logger.info("🔍 DEBUG: Executing endpoint: {} with parameters: {}", endpoint.getName(), parameters);

        // Null checks
        if (endpoint == null) {
            logger.error("❌ ERROR: Endpoint is null!");
            throw new IllegalArgumentException("Endpoint cannot be null");
        }

        SoapEndpoint reloadedEndpoint = soapEndpointRepository.findById(endpoint.getId()).orElse(endpoint);

        if (reloadedEndpoint.getRestCalls() == null || reloadedEndpoint.getRestCalls().isEmpty()) {
            logger.error("❌ ERROR: No REST calls configured for endpoint: {}", reloadedEndpoint.getName());
            throw new IllegalStateException("No REST calls configured");
        }

        Map<String, Object> results = new ConcurrentHashMap<>();

        // Execute REST calls
        for (RestCall restCall : reloadedEndpoint.getRestCalls()) {
            logger.info("🔍 DEBUG: Processing REST call: sequenceOrder={}, template={}",
                    restCall.getSequenceOrder(), restCall.getUrlTemplate());

            String url = buildUrl(restCall.getUrlTemplate(), parameters);
            logger.info("🌐 DEBUG: Built URL: {}", url);

            try {
                WebClient webClient = webClientBuilder.build();
                logger.info("🔍 DEBUG: Making REST call to: {}", url);

                String response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                logger.info("✅ DEBUG: REST API response received: {} characters", response != null ? response.length() : 0);

                if (response != null) {
                    logger.info("📄 DEBUG: First 200 chars of response: {}",
                            response.length() > 200 ? response.substring(0, 200) + "..." : response);
                }

                Map<String, Object> callResult = new HashMap<>();
                callResult.put("statusCode", 200);
                callResult.put("body", response);

                results.put("call_" + restCall.getSequenceOrder(), callResult);

            } catch (Exception e) {
                logger.error("❌ ERROR: REST call failed for URL {}: {}", url, e.getMessage(), e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("statusCode", 500);
                errorResult.put("body", "{\"error\":\"" + e.getMessage() + "\"}");
                results.put("call_" + restCall.getSequenceOrder(), errorResult);
            }
        }

        logger.info("🔍 DEBUG: All REST calls completed. Results: {}", results.keySet());

        // Apply mappings to transform the results
        return applyMappings(reloadedEndpoint, results);
    }

    private String buildUrl(String template, Map<String, Object> parameters) {
        logger.info("🔍 DEBUG: Building URL from template: {} with parameters: {}", template, parameters);

        String url = template;
        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            String placeholder = "${" + param.getKey() + "}";
            String value = String.valueOf(param.getValue());
            url = url.replace(placeholder, value);
            logger.info("🔍 DEBUG: Replaced {} with {} in URL", placeholder, value);
        }

        logger.info("🌐 DEBUG: Final built URL: {}", url);
        return url;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> applyMappings(SoapEndpoint endpoint, Map<String, Object> results) {
        logger.info("🔍 DEBUG: Applying mappings for endpoint: {}", endpoint.getName());

        try {
            Map<String, Object> finalResult = new ConcurrentHashMap<>();

            // Get the first REST call result
            Object call1 = results.get("call_1");
            logger.info("🔍 DEBUG: Retrieved call_1 result: {}", call1 != null ? "Found" : "NULL");

            if (call1 instanceof Map) {
                Map<String, Object> call1Map = (Map<String, Object>) call1;
                String jsonBody = (String) call1Map.get("body");

                logger.info("🔍 DEBUG: JSON body length: {}", jsonBody != null ? jsonBody.length() : "NULL");

                if (jsonBody != null) {
                    logger.info("📄 DEBUG: JSON body preview: {}",
                            jsonBody.length() > 300 ? jsonBody.substring(0, 300) + "..." : jsonBody);

                    // Parse JSON response
                    Map<String, Object> jsonData = objectMapper.readValue(jsonBody, Map.class);
                    logger.info("🔍 DEBUG: Parsed JSON keys: {}", jsonData.keySet());

                    // Extract first item from items array
                    if (jsonData.containsKey("items") && jsonData.get("items") instanceof java.util.List) {
                        java.util.List<?> items = (java.util.List<?>) jsonData.get("items");
                        logger.info("🔍 DEBUG: Found items array with {} elements", items.size());

                        if (!items.isEmpty() && items.get(0) instanceof Map) {
                            Map<String, Object> item = (Map<String, Object>) items.get(0);
                            logger.info("🔍 DEBUG: First item keys: {}", item.keySet());

                            // Map fields
                            Map<String, Object> personnelData = new ConcurrentHashMap<>();
                            personnelData.put("serviceNumber", item.get("sno"));
                            personnelData.put("name", item.get("p_name"));
                            personnelData.put("rankCode", item.get("rankcd"));
                            personnelData.put("rankType", item.get("ranktype"));
                            personnelData.put("rankDate", item.get("rankdt"));
                            personnelData.put("unitCode", item.get("unitcd"));
                            personnelData.put("tradeCode", item.get("trdcd"));
                            personnelData.put("dateOfBirth", item.get("dob"));
                            personnelData.put("gender", item.get("sex"));
                            personnelData.put("maritalStatus", item.get("mstatus"));
                            personnelData.put("enrollmentDate", item.get("enrldt"));
                            personnelData.put("panNumber", item.get("pan"));
                            personnelData.put("irlaNumber", item.get("irla"));
                            personnelData.put("category", item.get("cat"));

                            logger.info("✅ DEBUG: Mapped personnel data: {}", personnelData);
                            finalResult.put("personnelData", personnelData);
                        } else {
                            logger.warn("⚠️ WARNING: Items array is empty or first item is not a Map");
                        }
                    } else {
                        logger.warn("⚠️ WARNING: No 'items' key found or it's not a List. Available keys: {}", jsonData.keySet());
                    }
                } else {
                    logger.error("❌ ERROR: JSON body is null");
                }
            } else {
                logger.error("❌ ERROR: call_1 is not a Map. Type: {}", call1 != null ? call1.getClass() : "null");
            }

            logger.info("✅ DEBUG: Final mapping result: {}", finalResult);
            return finalResult;

        } catch (Exception e) {
            logger.error("❌ ERROR: Exception in applyMappings: {}", e.getMessage(), e);
            return results; // Return original results if mapping fails
        }
    }
}
