
package com.networkagent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networkagent.dto.DiagnosisResponse;
import com.networkagent.dto.NetworkLogRequest;
import com.networkagent.exception.AiServiceException;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OllamaApiClientTest {

    private HttpServer mockServer;
    private OllamaApiClient client;
    private ObjectMapper objectMapper;
    private NetworkLogRequest request;

    @BeforeEach
    void setUp() throws IOException {

        // Port 0 chooses an available local test port.
        mockServer = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        mockServer.start();

        int port = mockServer.getAddress().getPort();

        objectMapper = new ObjectMapper();

        client = new OllamaApiClient(
                RestClient.builder(),
                objectMapper,
                "http://127.0.0.1:" + port,
                "qwen3:4b-instruct"
        );

        request = new NetworkLogRequest(
                "Office-AP-01",
                "ACCESS_POINT",
                "RADIUS authentication timeout"
        );
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    // Make the fake Ollama server return JSON.
    private void mockResponse(int status, String responseBody) {

        mockServer.createContext("/api/chat", exchange -> {

            // Consume the incoming request.
            exchange.getRequestBody().readAllBytes();

            byte[] bytes = responseBody.getBytes(
                    StandardCharsets.UTF_8
            );

            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "application/json"
            );

            exchange.sendResponseHeaders(status, bytes.length);

            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
    }

    private String completedOllamaResponse(
            String diagnosisJson
    ) throws Exception {

        return objectMapper.writeValueAsString(
                Map.of(
                        "done", true,
                        "message", Map.of(
                                "role", "assistant",
                                "content", diagnosisJson
                        )
                )
        );
    }

    @Test
    void analyze_shouldParseSuccessfulDiagnosis()
            throws Exception {

        String diagnosisJson = objectMapper.writeValueAsString(
                Map.of(
                        "problemSummary",
                        "RADIUS authentication timeout",

                        "possibleRootCauses",
                        List.of("RADIUS server unavailable"),

                        "severity", "MEDIUM",

                        "recommendedSteps",
                        List.of("Verify RADIUS connectivity")
                )
        );

        mockResponse(
                200,
                completedOllamaResponse(diagnosisJson)
        );

        DiagnosisResponse diagnosis = client.analyze(request);

        assertNotNull(diagnosis);
        assertEquals(
                "RADIUS authentication timeout",
                diagnosis.problemSummary()
        );
        assertEquals("MEDIUM", diagnosis.severity());
        assertEquals(
                "RADIUS server unavailable",
                diagnosis.possibleRootCauses().get(0)
        );
    }

    @Test
    void analyze_shouldRejectMalformedDiagnosis()
            throws Exception {

        // Ollama reports success, but its generated text is not JSON.
        mockResponse(
                200,
                completedOllamaResponse("This is not JSON")
        );

        assertThrows(
                AiServiceException.class,
                () -> client.analyze(request)
        );
    }

    @Test
    void analyze_shouldHandleUpstreamServerFailure() {

        // Simulate an HTTP 500 returned by the model server.
        mockResponse(
                500,
                "{\"error\":\"model unavailable\"}"
        );

        assertThrows(
                AiServiceException.class,
                () -> client.analyze(request)
        );
    }

    @Test
    void analyze_shouldRejectIncompleteResponse() {

        mockResponse(
                200,
                """
                {
                  "done": false,
                  "message": {
                    "role": "assistant",
                    "content": ""
                  }
                }
                """
        );

        assertThrows(
                AiServiceException.class,
                () -> client.analyze(request)
        );
    }
}
