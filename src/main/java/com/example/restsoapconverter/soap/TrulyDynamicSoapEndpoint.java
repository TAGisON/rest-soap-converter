
package com.example.restsoapconverter.soap;

import com.example.restsoapconverter.entity.SoapEndpoint;
import com.example.restsoapconverter.service.ExecutionEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Endpoint
@Component
public class TrulyDynamicSoapEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(TrulyDynamicSoapEndpoint.class);
    private final Map<String, SoapEndpoint> registeredEndpoints = new ConcurrentHashMap<>();

    @Autowired
    private ExecutionEngineService executionEngineService;

    public void registerEndpoint(SoapEndpoint endpoint) {
        String key = endpoint.getNamespace() + "#" + endpoint.getOperationName();
        registeredEndpoints.put(key, endpoint);
        logger.info("Registered dynamic endpoint: {} -> {}", key, endpoint.getName());
    }

    public void unregisterEndpoint(SoapEndpoint endpoint) {
        String key = endpoint.getNamespace() + "#" + endpoint.getOperationName();
        registeredEndpoints.remove(key);
        logger.info("Unregistered dynamic endpoint: {}", key);
    }

    // ============= EXISTING PERSONNEL ENDPOINTS =============

    @SoapAction("http://afcao.personnel.service/GetPersonnelData")
    @PayloadRoot(namespace = "http://afcao.personnel.service", localPart = "GetPersonnelDataRequest")
    @ResponsePayload
    public Element handleGetPersonnelDataRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPersonnelData request");
        return handleDynamicRequest(request, "http://afcao.personnel.service", "GetPersonnelData");
    }

    @SoapAction("http://afcao.rankhistory.service/GetRankHistoryData")
    @PayloadRoot(namespace = "http://afcao.rankhistory.service", localPart = "GetRankHistoryDataRequest")
    @ResponsePayload
    public Element handleGetRankHistoryDataRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetRankHistoryData request");
        return handleDynamicRequest(request, "http://afcao.rankhistory.service", "GetRankHistoryData");
    }

    // ============= OPW PAYSLIP ENDPOINTS =============

    @SoapAction("http://afcao.payslip.opw.totalcredit.service/GetTotalCreditOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.totalcredit.service", localPart = "GetTotalCreditOPWRequest")
    @ResponsePayload
    public Element handleGetTotalCreditOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalCreditOPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.opw.totalcredit.service", "GetTotalCreditOPW", "gross_entitlement", "0");
    }

    @SoapAction("http://afcao.payslip.opw.totaldebit.service/GetTotalDebitOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.totaldebit.service", localPart = "GetTotalDebitOPWRequest")
    @ResponsePayload
    public Element handleGetTotalDebitOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalDebitOPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.opw.totaldebit.service", "GetTotalDebitOPW", "gross_deductions", "0");
    }

    @SoapAction("http://afcao.payslip.opw.netpay.service/GetNetPayOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.netpay.service", localPart = "GetNetPayOPWRequest")
    @ResponsePayload
    public Element handleGetNetPayOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetNetPayOPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.opw.netpay.service", "GetNetPayOPW", "net_entitlement", "0");
    }

    // ============= APW PAYSLIP ENDPOINTS =============

    @SoapAction("http://afcao.payslip.apw.totalcredit.service/GetTotalCreditAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.totalcredit.service", localPart = "GetTotalCreditAPWRequest")
    @ResponsePayload
    public Element handleGetTotalCreditAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalCreditAPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.apw.totalcredit.service", "GetTotalCreditAPW", "gross_entitlement", "1");
    }

    @SoapAction("http://afcao.payslip.apw.totaldebit.service/GetTotalDebitAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.totaldebit.service", localPart = "GetTotalDebitAPWRequest")
    @ResponsePayload
    public Element handleGetTotalDebitAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalDebitAPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.apw.totaldebit.service", "GetTotalDebitAPW", "gross_deductions", "1");
    }

    @SoapAction("http://afcao.payslip.apw.netpay.service/GetNetPayAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.netpay.service", localPart = "GetNetPayAPWRequest")
    @ResponsePayload
    public Element handleGetNetPayAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetNetPayAPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.apw.netpay.service", "GetNetPayAPW", "net_entitlement", "1");
    }

    // ============= CPW PAYSLIP ENDPOINTS =============

    @SoapAction("http://afcao.payslip.cpw.totalcredit.service/GetTotalCreditCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.totalcredit.service", localPart = "GetTotalCreditCPWRequest")
    @ResponsePayload
    public Element handleGetTotalCreditCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalCreditCPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.cpw.totalcredit.service", "GetTotalCreditCPW", "gross_entitlement", "2");
    }

    @SoapAction("http://afcao.payslip.cpw.totaldebit.service/GetTotalDebitCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.totaldebit.service", localPart = "GetTotalDebitCPWRequest")
    @ResponsePayload
    public Element handleGetTotalDebitCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalDebitCPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.cpw.totaldebit.service", "GetTotalDebitCPW", "gross_deductions", "2");
    }

    @SoapAction("http://afcao.payslip.cpw.netpay.service/GetNetPayCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.netpay.service", localPart = "GetNetPayCPWRequest")
    @ResponsePayload
    public Element handleGetNetPayCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetNetPayCPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.cpw.netpay.service", "GetNetPayCPW", "net_entitlement", "2");
    }

    // ============= SERVICE NUMBER CHECK ENDPOINTS =============

    @SoapAction("http://afcao.payslip.opw.check.service/CheckServiceNoOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.check.service", localPart = "CheckServiceNoOPWRequest")
    @ResponsePayload
    public Element handleCheckServiceNoOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckServiceNoOPW request");
        return handleServiceCheckRequestWithCategory(request, "http://afcao.payslip.opw.check.service", "CheckServiceNoOPW", "0");
    }

    @SoapAction("http://afcao.payslip.apw.check.service/CheckServiceNoAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.check.service", localPart = "CheckServiceNoAPWRequest")
    @ResponsePayload
    public Element handleCheckServiceNoAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckServiceNoAPW request");
        return handleServiceCheckRequestWithCategory(request, "http://afcao.payslip.apw.check.service", "CheckServiceNoAPW", "1");
    }

    @SoapAction("http://afcao.payslip.cpw.check.service/CheckServiceNoCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.check.service", localPart = "CheckServiceNoCPWRequest")
    @ResponsePayload
    public Element handleCheckServiceNoCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckServiceNoCPW request");
        return handleServiceCheckRequestWithCategory(request, "http://afcao.payslip.cpw.check.service", "CheckServiceNoCPW", "2");
    }

    // ============= NEW TPIN CHECK ENDPOINTS =============

    @SoapAction("http://afcao.tpin.opw.check.service/CheckTPINOPW")
    @PayloadRoot(namespace = "http://afcao.tpin.opw.check.service", localPart = "CheckTPINOPWRequest")
    @ResponsePayload
    public Element handleCheckTPINOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckTPINOPW request");
        return handleTPINCheckRequestWithCategory(request, "http://afcao.tpin.opw.check.service", "CheckTPINOPW", "0");
    }

    @SoapAction("http://afcao.tpin.apw.check.service/CheckTPINAPW")
    @PayloadRoot(namespace = "http://afcao.tpin.apw.check.service", localPart = "CheckTPINAPWRequest")
    @ResponsePayload
    public Element handleCheckTPINAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckTPINAPW request");
        return handleTPINCheckRequestWithCategory(request, "http://afcao.tpin.apw.check.service", "CheckTPINAPW", "1");
    }

    @SoapAction("http://afcao.tpin.cpw.check.service/CheckTPINCPW")
    @PayloadRoot(namespace = "http://afcao.tpin.cpw.check.service", localPart = "CheckTPINCPWRequest")
    @ResponsePayload
    public Element handleCheckTPINCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckTPINCPW request");
        return handleTPINCheckRequestWithCategory(request, "http://afcao.tpin.cpw.check.service", "CheckTPINCPW", "2");
    }

    // ============= TPIN CHECK PROCESSING LOGIC =============

    private Element handleTPINCheckRequestWithCategory(Element request, String namespace, String operationName, String category) {
        try {
            logger.info("🔐 Processing TPIN check request: operation={}, category={}", operationName, category);

            // Extract parameters (serviceNumber and tpin expected)
            Map<String, Object> parameters = extractParametersDynamically(request);
            logger.info("🔍 Extracted parameters from request: {}", parameters);

            // Validate required parameters
            if (!parameters.containsKey("serviceNumber")) {
                logger.error("❌ Missing required parameter: serviceNumber");
                return createSimpleResponse(namespace, operationName, "1"); // Return 1 for missing params
            }

            if (!parameters.containsKey("tpin")) {
                logger.error("❌ Missing required parameter: tpin");
                return createSimpleResponse(namespace, operationName, "1"); // Return 1 for missing params
            }

            String inputTpin = (String) parameters.get("tpin");
            String serviceNumber = (String) parameters.get("serviceNumber");

            // Validate TPIN format (should be 4 digits)
            if (inputTpin == null || inputTpin.trim().isEmpty()) {
                logger.error("❌ TPIN is null or empty");
                return createSimpleResponse(namespace, operationName, "1");
            }

            inputTpin = inputTpin.trim();
            if (!inputTpin.matches("\\d{4}")) {
                logger.error("❌ TPIN format invalid: '{}' (must be 4 digits)", inputTpin);
                return createSimpleResponse(namespace, operationName, "1");
            }

            logger.info("🔐 TPIN validation request for serviceNumber: {}, category: {}", serviceNumber, category);

            // Add category for API call
            parameters.put("category", category);
            logger.info("🔍 Added auto-category: {}. Final parameters: {}", category, parameters);

            // Find registered endpoint
            String key = namespace + "#" + operationName;
            SoapEndpoint endpoint = registeredEndpoints.get(key);

            if (endpoint == null) {
                logger.error("❌ No registered endpoint found for: {}", key);
                logger.info("Available endpoints: {}", registeredEndpoints.keySet());
                return createSimpleResponse(namespace, operationName, "1"); // Return 1 for endpoint not found
            }

            // Execute REST call to get TPIN details
            Map<String, Object> result = executionEngineService.executeEndpoint(endpoint, parameters);

            // Check TPIN match
            String checkResult = checkTPINMatch(result, inputTpin, serviceNumber, category);
            logger.info("✅ TPIN check result: {} for serviceNumber: {}, category: {} (0=match, 1=no match/error)",
                    checkResult, serviceNumber, category);

            return createSimpleResponse(namespace, operationName, checkResult);

        } catch (Exception e) {
            logger.error("❌ Error processing TPIN check request {}: {}", operationName, e.getMessage(), e);
            return createSimpleResponse(namespace, operationName, "1"); // Return 1 for any error
        }
    }

    @SuppressWarnings("unchecked")
    private String checkTPINMatch(Map<String, Object> result, String inputTpin, String serviceNumber, String category) {
        try {
            logger.info("🔐 Checking TPIN match for serviceNumber: {}, category: {}", serviceNumber, category);

            // Navigate through the result structure
            if (result.containsKey("call_1")) {
                Map<String, Object> call1 = (Map<String, Object>) result.get("call_1");

                // Check status code first
                if (call1.containsKey("statusCode")) {
                    Integer statusCode = (Integer) call1.get("statusCode");
                    logger.info("🔍 API response status code: {}", statusCode);

                    if (statusCode != 200) {
                        logger.warn("⚠️ API returned non-200 status: {} - TPIN check failed", statusCode);
                        return "1"; // API error = TPIN check failed
                    }
                }

                if (call1.containsKey("body")) {
                    String jsonBody = (String) call1.get("body");
                    logger.info("🔍 API response body length: {}", jsonBody != null ? jsonBody.length() : "NULL");

                    if (jsonBody == null || jsonBody.trim().isEmpty()) {
                        logger.warn("⚠️ API response body is null or empty - TPIN check failed");
                        return "1"; // Empty response = TPIN check failed
                    }

                    // Parse JSON
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> jsonData = objectMapper.readValue(jsonBody, Map.class);
                    logger.info("🔍 Parsed JSON keys: {}", jsonData.keySet());

                    // Check if items array exists and has data
                    if (jsonData.containsKey("items") && jsonData.get("items") instanceof java.util.List) {
                        java.util.List<?> items = (java.util.List<?>) jsonData.get("items");
                        logger.info("🔍 Items array size: {}", items.size());

                        if (!items.isEmpty() && items.get(0) instanceof Map) {
                            Map<String, Object> item = (Map<String, Object>) items.get(0);
                            logger.info("🔍 Token item keys: {}", item.keySet());

                            // Extract TPIN from API response
                            if (item.containsKey("ivrs_pin")) {
                                Object apiTpinObj = item.get("ivrs_pin");
                                if (apiTpinObj != null) {
                                    String apiTpin = String.valueOf(apiTpinObj).trim();
                                    logger.info("🔐 TPIN comparison: input='{}', api='{}'", inputTpin, apiTpin);

                                    // Compare TPINs (exact match required)
                                    if (inputTpin.equals(apiTpin)) {
                                        logger.info("✅ TPIN MATCH SUCCESS for serviceNumber: {}, category: {}", serviceNumber, category);
                                        return "0"; // Match
                                    } else {
                                        logger.warn("❌ TPIN MISMATCH: input='{}' != api='{}' for serviceNumber: {}, category: {}",
                                                inputTpin, apiTpin, serviceNumber, category);
                                        return "1"; // Mismatch
                                    }
                                } else {
                                    logger.warn("⚠️ API returned null ivrs_pin for serviceNumber: {}, category: {}", serviceNumber, category);
                                    return "1"; // Null TPIN = failed
                                }
                            } else {
                                logger.warn("⚠️ No 'ivrs_pin' key found in API response. Available keys: {}", item.keySet());
                                return "1"; // Missing TPIN field = failed
                            }
                        } else {
                            logger.warn("⚠️ Items array is empty or first item is not a Map. Items size: {}", items.size());
                            return "1"; // No valid data = failed
                        }
                    } else {
                        logger.warn("⚠️ No 'items' key found or it's not a List. Available keys: {}", jsonData.keySet());
                        return "1"; // Invalid structure = failed
                    }
                } else {
                    logger.warn("⚠️ No 'body' key found in call_1 result");
                    return "1"; // No body = failed
                }
            } else {
                logger.warn("⚠️ No 'call_1' key found in result. Available keys: {}", result.keySet());
                return "1"; // No call result = failed
            }

        } catch (Exception e) {
            logger.error("❌ Error checking TPIN match: {}", e.getMessage(), e);
            return "1"; // Any error = failed
        }
    }

    // ============= SERVICE CHECK PROCESSING LOGIC =============

    private Element handleServiceCheckRequestWithCategory(Element request, String namespace, String operationName, String category) {
        try {
            logger.info("🔍 Processing service check request: operation={}, category={}", operationName, category);

            // Extract parameters (only serviceNumber expected)
            Map<String, Object> parameters = extractParametersDynamically(request);
            logger.info("🔍 Extracted parameters from request: {}", parameters);

            if (!parameters.containsKey("serviceNumber")) {
                logger.error("❌ Missing required parameter: serviceNumber");
                return createSimpleResponse(namespace, operationName, "1"); // Return 1 for missing params
            }

            // Add category automatically based on endpoint type
            parameters.put("category", category);
            logger.info("🔍 Added auto-category: {}. Final parameters: {}", category, parameters);

            // Find registered endpoint
            String key = namespace + "#" + operationName;
            SoapEndpoint endpoint = registeredEndpoints.get(key);

            if (endpoint == null) {
                logger.error("❌ No registered endpoint found for: {}", key);
                logger.info("Available endpoints: {}", registeredEndpoints.keySet());
                return createSimpleResponse(namespace, operationName, "1"); // Return 1 for endpoint not found
            }

            // Execute REST call
            Map<String, Object> result = executionEngineService.executeEndpoint(endpoint, parameters);

            // Check if service number exists
            String checkResult = checkServiceNumberExists(result);
            logger.info("✅ Service check result: {} for category {} (0=found, 1=not found)", checkResult, category);

            return createSimpleResponse(namespace, operationName, checkResult);

        } catch (Exception e) {
            logger.error("❌ Error processing service check request {}: {}", operationName, e.getMessage(), e);
            return createSimpleResponse(namespace, operationName, "1"); // Return 1 for any error
        }
    }

    @SuppressWarnings("unchecked")
    private String checkServiceNumberExists(Map<String, Object> result) {
        try {
            logger.info("🔍 Checking if service number exists in API response");

            // Navigate through the result structure
            if (result.containsKey("call_1")) {
                Map<String, Object> call1 = (Map<String, Object>) result.get("call_1");

                // Check status code first
                if (call1.containsKey("statusCode")) {
                    Integer statusCode = (Integer) call1.get("statusCode");
                    logger.info("🔍 API response status code: {}", statusCode);

                    if (statusCode != 200) {
                        logger.warn("⚠️ API returned non-200 status: {}", statusCode);
                        return "1"; // API error = service not found
                    }
                }

                if (call1.containsKey("body")) {
                    String jsonBody = (String) call1.get("body");
                    logger.info("🔍 API response body length: {}", jsonBody != null ? jsonBody.length() : "NULL");

                    if (jsonBody == null || jsonBody.trim().isEmpty()) {
                        logger.warn("⚠️ API response body is null or empty");
                        return "1"; // Empty response = service not found
                    }

                    // Parse JSON
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> jsonData = objectMapper.readValue(jsonBody, Map.class);
                    logger.info("🔍 Parsed JSON keys: {}", jsonData.keySet());

                    // Check if items array exists and has data
                    if (jsonData.containsKey("items") && jsonData.get("items") instanceof java.util.List) {
                        java.util.List<?> items = (java.util.List<?>) jsonData.get("items");
                        logger.info("🔍 Items array size: {}", items.size());

                        if (!items.isEmpty()) {
                            // Items exist = service number found
                            logger.info("✅ Service number exists - items array has {} entries", items.size());
                            return "0"; // Found
                        } else {
                            // Items array is empty = service number not found
                            logger.info("⚠️ Service number not found - items array is empty");
                            return "1"; // Not found
                        }
                    } else {
                        logger.warn("⚠️ No 'items' key found or it's not a List. Available keys: {}", jsonData.keySet());
                        return "1"; // Invalid structure = service not found
                    }
                } else {
                    logger.warn("⚠️ No 'body' key found in call_1 result");
                    return "1"; // No body = service not found
                }
            } else {
                logger.warn("⚠️ No 'call_1' key found in result. Available keys: {}", result.keySet());
                return "1"; // No call result = service not found
            }

        } catch (Exception e) {
            logger.error("❌ Error checking service number existence: {}", e.getMessage(), e);
            return "1"; // Any error = service not found
        }
    }

    // ============= PAYSLIP PROCESSING LOGIC =============

    private Element handlePayslipRequestWithCategory(Element request, String namespace, String operationName, String jsonKey, String category) {
        try {
            logger.info("🔍 Processing payslip request with auto-category: operation={}, jsonKey={}, category={}", operationName, jsonKey, category);

            // Extract parameters (only serviceNumber expected)
            Map<String, Object> parameters = extractParametersDynamically(request);
            logger.info("🔍 Extracted parameters from request: {}", parameters);

            if (!parameters.containsKey("serviceNumber")) {
                logger.error("❌ Missing required parameter: serviceNumber");
                return createSimpleResponse(namespace, operationName, "-1");
            }

            // Add category automatically based on endpoint type
            parameters.put("category", category);
            logger.info("🔍 Added auto-category: {}. Final parameters: {}", category, parameters);

            // Find registered payslip endpoint
            String key = namespace + "#" + operationName;
            SoapEndpoint endpoint = registeredEndpoints.get(key);

            if (endpoint == null) {
                logger.error("❌ No registered endpoint found for: {}", key);
                logger.info("Available endpoints: {}", registeredEndpoints.keySet());
                return createSimpleResponse(namespace, operationName, "-1");
            }

            // Execute REST call
            Map<String, Object> result = executionEngineService.executeEndpoint(endpoint, parameters);

            // Extract specific value from result
            String value = extractPayslipValue(result, jsonKey);
            logger.info("✅ Extracted {} value: {} for category {}", jsonKey, value, category);

            return createSimpleResponse(namespace, operationName, value);

        } catch (Exception e) {
            logger.error("❌ Error processing payslip request with category {}: {}", operationName, e.getMessage(), e);
            return createSimpleResponse(namespace, operationName, "-1");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractPayslipValue(Map<String, Object> result, String jsonKey) {
        try {
            // Navigate through the result structure to find the value
            if (result.containsKey("call_1")) {
                Map<String, Object> call1 = (Map<String, Object>) result.get("call_1");
                if (call1.containsKey("body")) {
                    String jsonBody = (String) call1.get("body");

                    // Parse JSON
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> jsonData = objectMapper.readValue(jsonBody, Map.class);

                    // Check if items array exists and has data
                    if (jsonData.containsKey("items") && jsonData.get("items") instanceof java.util.List) {
                        java.util.List<?> items = (java.util.List<?>) jsonData.get("items");

                        if (!items.isEmpty() && items.get(0) instanceof Map) {
                            Map<String, Object> item = (Map<String, Object>) items.get(0);

                            // Get the specific value
                            if (item.containsKey(jsonKey)) {
                                Object value = item.get(jsonKey);
                                if (value != null) {
                                    logger.info("🔍 Successfully found {} = {} in JSON response", jsonKey, value);
                                    return String.valueOf(value);
                                } else {
                                    logger.warn("⚠️ Found {} key but value is null", jsonKey);
                                }
                            } else {
                                logger.warn("⚠️ JSON item does not contain key: {}. Available keys: {}", jsonKey, item.keySet());
                            }
                        } else {
                            logger.warn("⚠️ Items array is empty or first item is not a Map. Items size: {}", items.size());
                        }
                    } else {
                        logger.warn("⚠️ No 'items' key found or it's not a List. Available keys: {}", jsonData.keySet());
                    }
                } else {
                    logger.warn("⚠️ No 'body' key found in call_1 result");
                }
            } else {
                logger.warn("⚠️ No 'call_1' key found in result. Available keys: {}", result.keySet());
            }

            logger.warn("⚠️ Could not find {} in result structure", jsonKey);
            return "-1";

        } catch (Exception e) {
            logger.error("❌ Error extracting payslip value for {}: {}", jsonKey, e.getMessage(), e);
            return "-1";
        }
    }

    private Element createSimpleResponse(String namespace, String operationName, String returnValue) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Create response element: <OperationResponse xmlns="namespace">
            Element response = doc.createElementNS(namespace, operationName + "Response");
            doc.appendChild(response);

            // Create return element: <return>value</return>
            Element returnElement = doc.createElement("return");
            returnElement.setTextContent(returnValue);
            response.appendChild(returnElement);

            logger.info("✅ Created simple response for {}: {}", operationName, returnValue);
            return response;

        } catch (Exception e) {
            logger.error("❌ Error creating simple response: {}", e.getMessage(), e);
            // Return minimal error response
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.newDocument();

                Element response = doc.createElementNS(namespace, operationName + "Response");
                doc.appendChild(response);

                Element returnElement = doc.createElement("return");
                returnElement.setTextContent("-1");
                response.appendChild(returnElement);

                return response;
            } catch (Exception ex) {
                logger.error("❌ Critical error creating error response: {}", ex.getMessage());
                throw new RuntimeException("Failed to create SOAP response", ex);
            }
        }
    }

    // ============= EXISTING DYNAMIC HANDLERS =============

    private Element handleDynamicRequest(Element request, String namespace, String operationName) throws Exception {
        String key = namespace + "#" + operationName;
        SoapEndpoint endpoint = registeredEndpoints.get(key);

        if (endpoint == null) {
            logger.error("❌ No registered endpoint found for: {}", key);
            return createErrorResponse(namespace, "Endpoint not found: " + operationName);
        }

        try {
            Map<String, Object> parameters = extractParametersDynamically(request);
            Map<String, Object> result = executionEngineService.executeEndpoint(endpoint, parameters);
            return createSoapResponse(namespace, operationName, result);
        } catch (Exception e) {
            logger.error("❌ Error executing endpoint {}: {}", operationName, e.getMessage(), e);
            return createErrorResponse(namespace, "Internal server error: " + e.getMessage());
        }
    }

    private Map<String, Object> extractParametersDynamically(Element request) {
        Map<String, Object> parameters = new ConcurrentHashMap<>();

        for (int i = 0; i < request.getChildNodes().getLength(); i++) {
            Node child = request.getChildNodes().item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String paramName = child.getLocalName();
                String paramValue = child.getTextContent();
                parameters.put(paramName, paramValue);
                logger.debug("🔍 Extracted param: {} = {}", paramName, paramValue);
            }
        }

        return parameters;
    }

    // Existing methods for complex responses...
    private Element createSoapResponse(String namespace, String operationName, Map<String, Object> result) throws Exception {
        // Implementation for complex responses (personnel, rank history, etc.)
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element response = doc.createElementNS(namespace, operationName + "Response");
        doc.appendChild(response);

        Element statusElem = doc.createElement("status");
        statusElem.setTextContent(result.getOrDefault("status", "200").toString());
        response.appendChild(statusElem);

        Element messageElem = doc.createElement("message");
        messageElem.setTextContent(result.getOrDefault("message", "Request processed successfully").toString());
        response.appendChild(messageElem);

        addDataElementsRecursively(doc, response, result);
        return response;
    }

    @SuppressWarnings("unchecked")
    private void addDataElementsRecursively(Document doc, Element parent, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if ("status".equals(entry.getKey()) || "message".equals(entry.getKey())) {
                continue;
            }

            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                Element nestedElement = doc.createElement(key);
                addDataElementsRecursively(doc, nestedElement, (Map<String, Object>) value);
                parent.appendChild(nestedElement);
            } else if (value instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) value;
                Element arrayElement = doc.createElement(key);

                for (Object item : list) {
                    if (item instanceof Map) {
                        Element itemElement = doc.createElement(key.replaceAll("s$", ""));
                        addDataElementsRecursively(doc, itemElement, (Map<String, Object>) item);
                        arrayElement.appendChild(itemElement);
                    } else {
                        Element itemElement = doc.createElement("item");
                        itemElement.setTextContent(String.valueOf(item));
                        arrayElement.appendChild(itemElement);
                    }
                }
                parent.appendChild(arrayElement);
            } else {
                Element simpleElement = doc.createElement(key);
                simpleElement.setTextContent(String.valueOf(value));
                parent.appendChild(simpleElement);
            }
        }
    }

    private Element createErrorResponse(String namespace, String errorMessage) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element fault = doc.createElementNS(namespace, "Fault");
        doc.appendChild(fault);

        Element faultCode = doc.createElement("faultCode");
        faultCode.setTextContent("Server");
        fault.appendChild(faultCode);

        Element faultString = doc.createElement("faultString");
        faultString.setTextContent(errorMessage);
        fault.appendChild(faultString);

        return fault;
    }
}
