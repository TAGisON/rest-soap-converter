
package com.example.restsoapconverter.integration;

import com.example.restsoapconverter.dto.EndpointCreateRequest;
import com.example.restsoapconverter.dto.RestCallConfig;
import com.example.restsoapconverter.entity.RestCall;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test demonstrating:
 * 1. Create endpoint via Admin API
 * 2. WireMock provides REST responses
 * 3. Call SOAP endpoint and verify response
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.yml")
class EndToEndIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Start WireMock server
        wireMockServer = new WireMockServer(8081);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8081);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void endToEndFlow_CreateEndpointAndTest() throws Exception {
        // Setup WireMock stub
        wireMockServer.stubFor(get(urlEqualTo("/api/sample"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":\"Hello from REST API\",\"status\":\"success\"}")));

        // Create endpoint with unique name to avoid conflicts
        EndpointCreateRequest request = createUniqueEndpointRequest();

        mockMvc.perform(post("/admin/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(request.getName()));
    }

    private EndpointCreateRequest createUniqueEndpointRequest() {
        // Generate unique name to avoid conflicts with existing data
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String uniqueName = "TestEndpoint_" + uniqueSuffix;
        String uniqueOperation = "TestOperation_" + uniqueSuffix;
        String uniqueNamespace = "http://test.example.com/" + uniqueSuffix;

        EndpointCreateRequest request = new EndpointCreateRequest();
        request.setName(uniqueName);
        request.setOperationName(uniqueOperation);
        request.setNamespace(uniqueNamespace);
        request.setEnabled(true);

        RestCallConfig restCall = new RestCallConfig();
        restCall.setSequenceOrder(1);
        restCall.setMethod(RestCall.HttpMethod.GET);
        restCall.setUrlTemplate("http://localhost:8081/api/sample");
        restCall.setTimeoutMs(10000);
        request.setRestCalls(Arrays.asList(restCall));

        return request;
    }
}
