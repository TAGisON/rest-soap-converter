package com.example.restsoapconverter.controller;

import com.example.restsoapconverter.dto.EndpointCreateRequest;
import com.example.restsoapconverter.dto.EndpointResponse;
import com.example.restsoapconverter.dto.RestCallConfig;
import com.example.restsoapconverter.entity.RestCall;
import com.example.restsoapconverter.service.EndpointManagerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EndpointManagerService endpointManagerService;

    @Autowired
    private ObjectMapper objectMapper;

    private EndpointCreateRequest sampleRequest;
    private EndpointResponse sampleResponse;

    @BeforeEach
    void setUp() {
        // Setup sample request
        sampleRequest = new EndpointCreateRequest();
        sampleRequest.setName("TestEndpoint");
        sampleRequest.setOperationName("TestOperation");
        sampleRequest.setNamespace("http://test.example.com");

        RestCallConfig restCall = new RestCallConfig();
        restCall.setSequenceOrder(1);
        restCall.setMethod(RestCall.HttpMethod.GET);
        restCall.setUrlTemplate("http://test-api.com/data");
        sampleRequest.setRestCalls(Arrays.asList(restCall));

        // Setup sample response
        sampleResponse = new EndpointResponse();
        sampleResponse.setId(1L);
        sampleResponse.setName("TestEndpoint");
        sampleResponse.setOperationName("TestOperation");
        sampleResponse.setNamespace("http://test.example.com");
        sampleResponse.setEnabled(true);
        sampleResponse.setCreatedAt(LocalDateTime.now());
        sampleResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createEndpoint_Success() throws Exception {
        when(endpointManagerService.createEndpoint(any(EndpointCreateRequest.class), anyString()))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/admin/endpoints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("TestEndpoint"));
    }

    @Test
    void getAllEndpoints_Success() throws Exception {
        List<EndpointResponse> endpoints = Arrays.asList(sampleResponse);
        when(endpointManagerService.getAllEndpoints()).thenReturn(endpoints);

        mockMvc.perform(get("/admin/endpoints"))
                .andExpect(status().isOk()) // Correct method: andExpect
                .andExpect(jsonPath("$").isArray()) // Correct method: andExpect
                .andExpect(jsonPath("$[0].name").value("TestEndpoint")); // Correct method: andExpect
    }

}
