
package com.networkagent.service;

import com.networkagent.client.OllamaApiClient;
import com.networkagent.dto.DiagnosisResponse;
import com.networkagent.dto.NetworkLogRequest;
import com.networkagent.exception.AiServiceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkAnalysisServiceTest {

    @Mock
    private OllamaApiClient ollamaApiClient;

    @Mock
    private DiagnosisValidator diagnosisValidator;

    @InjectMocks
    private NetworkAnalysisService analysisService;

    private NetworkLogRequest request;
    private DiagnosisResponse expectedDiagnosis;

    @BeforeEach
    void setUp() {

        request = new NetworkLogRequest(
                "Office-AP-01",
                "ACCESS_POINT",
                "Client disconnected due to authentication timeout"
        );

        expectedDiagnosis = new DiagnosisResponse(
                "RADIUS authentication timeout",
                List.of(
                        "RADIUS server unavailable",
                        "Network connectivity issue"
                ),
                "MEDIUM",
                List.of(
                        "Check RADIUS server connectivity",
                        "Review authentication server logs"
                )
        );
    }

    @Test
    void analyzeLog_shouldReturnDiagnosis_whenClientSucceeds() {

        // Arrange: configure the mocked AI client.
        when(ollamaApiClient.analyze(request))
                .thenReturn(expectedDiagnosis);

        // Act: invoke the real service method.
        DiagnosisResponse actual =
                analysisService.analyzeLog(request);

        // Assert: verify the returned diagnosis.
        assertNotNull(actual);
        assertEquals(
                "RADIUS authentication timeout",
                actual.problemSummary()
        );
        assertEquals("MEDIUM", actual.severity());

        assertEquals(2, actual.possibleRootCauses().size());
        assertEquals(2, actual.recommendedSteps().size());

        // Verify the client was called exactly once.
        verify(ollamaApiClient, times(1)).analyze(request);
    }


    @Test
    void analyzeLog_shouldPropagateException_whenAiFails() {

        // Arrange: simulate Ollama being unavailable.
        when(ollamaApiClient.analyze(request))
                .thenThrow(
                        new AiServiceException(
                                "Local AI analysis unavailable"
                        )
                );

        // Act and Assert: verify the exception propagates.
        AiServiceException exception = assertThrows(
                AiServiceException.class,
                () -> analysisService.analyzeLog(request)
        );

        assertEquals(
                "Local AI analysis unavailable",
                exception.getMessage()
        );

        // Ollama should be called exactly once.
        verify(ollamaApiClient, times(1))
                .analyze(request);

        // No diagnosis was returned, so validation must not run.
        verifyNoInteractions(diagnosisValidator);
    }
}
