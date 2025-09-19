// 2. FILE: src/main/java/com/example/restsoapconverter/soap/DynamicSoapEndpoint.java

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Endpoint
@Component
public class DynamicSoapEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(DynamicSoapEndpoint.class);

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

    @PayloadRoot(namespace = "http://afcao.personnel.service", localPart = "GetPersonnelDataRequest")
    @ResponsePayload
    public Element handleGetPersonnelDataRequest(@RequestPayload Element request) throws Exception {
        String namespace = "http://afcao.personnel.service";
        String operationName = "GetPersonnelData";

        logger.info("Handling SOAP request: namespace={}, operation={}", namespace, operationName);

        return handleDynamicRequest(request, namespace, operationName);
    }

    private Element handleDynamicRequest(Element request, String namespace, String operationName) throws Exception {
        String key = namespace + "#" + operationName;
        SoapEndpoint endpoint = registeredEndpoints.get(key);

        if (endpoint == null) {
            logger.error("No registered endpoint found for: {}", key);
            return createErrorResponse(namespace, "Endpoint not found: " + operationName);
        }

        try {
            // Extract parameters from SOAP request
            Map<String, Object> parameters = extractParameters(request);

            // Execute the endpoint logic
            Map<String, Object> result = executionEngineService.executeEndpoint(endpoint, parameters);

            // Convert result to SOAP response
            return createSoapResponse(namespace, operationName, result);

        } catch (Exception e) {
            logger.error("Error executing endpoint {}: {}", operationName, e.getMessage(), e);
            return createErrorResponse(namespace, "Internal server error: " + e.getMessage());
        }
    }

    private Map<String, Object> extractParameters(Element request) {
        Map<String, Object> parameters = new ConcurrentHashMap<>();

        // Extract serviceNumber
        Node serviceNumberNode = findChildNode(request, "serviceNumber");
        if (serviceNumberNode != null) {
            parameters.put("serviceNumber", serviceNumberNode.getTextContent());
        }

        // Extract category
        Node categoryNode = findChildNode(request, "category");
        if (categoryNode != null) {
            parameters.put("category", categoryNode.getTextContent());
        }

        logger.debug("Extracted parameters: {}", parameters);
        return parameters;
    }

    private Node findChildNode(Element parent, String nodeName) {
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            Node child = parent.getChildNodes().item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && 
                (child.getLocalName().equals(nodeName) || child.getNodeName().equals(nodeName))) {
                return child;
            }
        }
        return null;
    }

    private Element createSoapResponse(String namespace, String operationName, Map<String, Object> result) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Create response element
        Element response = doc.createElementNS(namespace, operationName + "Response");
        doc.appendChild(response);

        // Add status
        Element statusElem = doc.createElement("status");
        statusElem.setTextContent("200");
        response.appendChild(statusElem);

        // Add message
        Element messageElem = doc.createElement("message");
        messageElem.setTextContent("Personnel data retrieved successfully");
        response.appendChild(messageElem);

        // Add personnel data if available
        if (result.containsKey("personnelData")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> personnelData = (Map<String, Object>) result.get("personnelData");
            Element personnelElem = doc.createElement("personnelData");

            for (Map.Entry<String, Object> entry : personnelData.entrySet()) {
                Element fieldElem = doc.createElement(entry.getKey());
                fieldElem.setTextContent(String.valueOf(entry.getValue()));
                personnelElem.appendChild(fieldElem);
            }

            response.appendChild(personnelElem);
        }

        logger.info("Created SOAP response for operation: {}", operationName);
        return response;
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
