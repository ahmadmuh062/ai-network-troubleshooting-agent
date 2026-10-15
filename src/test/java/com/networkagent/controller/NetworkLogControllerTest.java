
package com.networkagent.controller;

import com.networkagent.dto.DiagnosisResponse;
import com.networkagent.dto.NetworkLogRequest;
import com.networkagent.exception.AiServiceException;
import com.networkagent.exception.GlobalExceptionHandler;
import com.networkagent.service.NetworkAnalysisService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NetworkLogController.class)
@Import(GlobalExceptionHandler.class)
class NetworkLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NetworkAnalysisService analysisService;

    @Test
    void health_shouldReturnUp() throws Exception {

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        verifyNoInteractions(analysisService);
    }

    @Test
    void analyzeLogs_shouldReturnDiagnosis() throws Exception {

        NetworkLogRequest request = new NetworkLogRequest(
                "Office-AP-01",
                "ACCESS_POINT",
                "RADIUS authentication timeout"
        );

        DiagnosisResponse diagnosis = new DiagnosisResponse(
                "RADIUS server did not respond",
                List.of("RADIUS server unreachable"),
                "MEDIUM",
                List.of("Check RADIUS connectivity")
        );

        // Mock the service response.
        when(analysisService.analyzeLog(any(NetworkLogRequest.class)))
                .thenReturn(diagnosis);

        mockMvc.perform(
                        post("/api/v1/logs/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemSummary")
                        .value("RADIUS server did not respond"))
                .andExpect(jsonPath("$.severity").value("MEDIUM"))
                .andExpect(jsonPath("$.possibleRootCauses[0]")
                        .value("RADIUS server unreachable"))
                .andExpect(jsonPath("$.recommendedSteps[0]")
                        .value("Check RADIUS connectivity"));

        verify(analysisService, times(1))
                .analyzeLog(any(NetworkLogRequest.class));
    }

    @Test
    void analyzeLogs_shouldReturn400_forInvalidRequest()
            throws Exception {

        NetworkLogRequest invalidRequest = new NetworkLogRequest(
                "",
                "ROUTER",
                ""
        );

        mockMvc.perform(
                        post("/api/v1/logs/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));

        // Invalid requests must never reach the AI layer.
        verifyNoInteractions(analysisService);
    }

    @Test
    void analyzeLogs_shouldReturn502_whenAiFails()
            throws Exception {

        when(analysisService.analyzeLog(any(NetworkLogRequest.class)))
                .thenThrow(
                        new AiServiceException("Ollama unavailable")
                );

        NetworkLogRequest request = new NetworkLogRequest(
                "Office-AP-01",
                "ACCESS_POINT",
                "RADIUS authentication timeout"
        );

        mockMvc.perform(
                        post("/api/v1/logs/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error")
                        .value("AI_SERVICE_ERROR"))
                .andExpect(jsonPath("$.status").value(502));

        verify(analysisService, times(1))
                .analyzeLog(any(NetworkLogRequest.class));
    }
}
