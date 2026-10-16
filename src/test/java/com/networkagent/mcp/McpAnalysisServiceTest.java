
package com.networkagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networkagent.dto.DiagnosisResponse;
import com.networkagent.dto.NetworkLogRequest;
import com.networkagent.exception.AiServiceException;
import com.networkagent.mcp.NetworkMcpClient;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpAnalysisServiceTest {

    private static final String ORIGINAL_LOG =
            "RADIUS server did not respond. Authentication timeout.";

    private HttpServer fakeOllama;
    private final Deque<String> responses = new ArrayDeque<>();

    private final ObjectMapper mapper = new ObjectMapper();

    private NetworkMcpClient mcpClient;
    private McpAnalysisService analysisService;
    private NetworkLogRequest request;

    @BeforeEach
    void setUp() throws IOException {

        // Use an available local port, not the real Ollama port.
        fakeOllama = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        fakeOllama.createContext("/api/chat", this::handleChat);
        fakeOllama.start();

        int port = fakeOllama.getAddress().getPort();

        // Mockito prevents real MCP calls.
        mcpClient = mock(NetworkMcpClient.class);

        analysisService = new McpAnalysisService(
                RestClient.builder(),
                mapper,
                mcpClient,
                new DiagnosisValidator(),
                "http://127.0.0.1:" + port,
                "qwen3:4b-instruct"
        );

        request = new NetworkLogRequest(
                "Office-AP-01",
                "ACCESS_POINT",
                ORIGINAL_LOG
        );
    }

    @AfterEach
    void tearDown() {
        fakeOllama.stop(0);
        responses.clear();
    }

    // Simulate the Ollama /api/chat endpoint.
    private void handleChat(HttpExchange exchange)
            throws IOException {

        byte[] output;
        int status;

        // Consume request data without logging sensitive content.
        exchange.getRequestBody().readAllBytes();

        synchronized (responses) {
            if (responses.isEmpty()) {
                status = 500;
                output = "{\"error\":\"No fake response configured\"}"
                        .getBytes(StandardCharsets.UTF_8);
            } else {
                status = 200;
                output = responses.removeFirst()
                        .getBytes(StandardCharsets.UTF_8);
            }
        }

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json"
        );

        exchange.sendResponseHeaders(status, output.length);

        try (var stream = exchange.getResponseBody()) {
            stream.write(output);
        }
    }

    // Wrap an assistant message in the Ollama response format.
    private String ollamaResponse(
            Map<String, Object> message,
            boolean done
    ) throws Exception {

        return mapper.writeValueAsString(Map.of(
                "model", "qwen3:4b-instruct",
                "done", done,
                "message", message
        ));
    }

    // Build a model-generated tool request.
    private String toolCall(
            String toolName,
            Map<String, Object> arguments
    ) throws Exception {

        Map<String, Object> function = Map.of(
                "name", toolName,
                "arguments", arguments
        );

        return ollamaResponse(
                Map.of(
                        "role", "assistant",
                        "content", "",
                        "tool_calls", List.of(
                                Map.of("function", function)
                        )
                ),
                true
        );
    }

    // Prepare a valid structured final diagnosis.
    private String validDiagnosisResponse() throws Exception {

        String diagnosis = mapper.writeValueAsString(Map.of(
                "problemSummary",
                "RADIUS authentication timeout",

                "possibleRootCauses",
                List.of("RADIUS server unreachable"),

                "severity",
                "MEDIUM",

                "recommendedSteps",
                List.of("Check RADIUS server connectivity")
        ));

        return ollamaResponse(
                Map.of(
                        "role", "assistant",
                        "content", diagnosis
                ),
                true
        );
    }

    private void enqueue(String response) {
        synchronized (responses) {
            responses.addLast(response);
        }
    }

    @Test
    void shouldExecuteApprovedToolAndReturnDiagnosis()
            throws Exception {

        enqueue(toolCall(
                "inspect_network_log",
                Map.of("logContent", ORIGINAL_LOG)
        ));

        enqueue(validDiagnosisResponse());

        when(mcpClient.inspectLog(ORIGINAL_LOG))
                .thenReturn(
                        "RADIUS_TIMEOUT: Authentication timeout detected"
                );

        DiagnosisResponse diagnosis =
                analysisService.analyze(request);

        assertEquals("MEDIUM", diagnosis.severity());
        assertEquals(
                "RADIUS authentication timeout",
                diagnosis.problemSummary()
        );

        verify(mcpClient, times(1))
                .inspectLog(ORIGINAL_LOG);
    }

    @Test
    void shouldRejectUnapprovedTool()
            throws Exception {

        // Simulate a model requesting an unauthorized function.
        enqueue(toolCall(
                "execute_command",
                Map.of("command", "whoami")
        ));

        assertThrows(
                AiServiceException.class,
                () -> analysisService.analyze(request)
        );

        // Absolutely no MCP execution should occur.
        verifyNoInteractions(mcpClient);
    }

    @Test
    void shouldRejectModifiedLogArguments()
            throws Exception {

        enqueue(toolCall(
                "inspect_network_log",
                Map.of("logContent", "Different log content")
        ));

        assertThrows(
                AiServiceException.class,
                () -> analysisService.analyze(request)
        );

        verifyNoInteractions(mcpClient);
    }

    @Test
    void shouldRejectMultipleToolRequests()
            throws Exception {

        Map<String, Object> function = Map.of(
                "name", "inspect_network_log",
                "arguments", Map.of(
                        "logContent", ORIGINAL_LOG
                )
        );

        enqueue(ollamaResponse(
                Map.of(
                        "role", "assistant",
                        "content", "",
                        "tool_calls", List.of(
                                Map.of("function", function),
                                Map.of("function", function)
                        )
                ),
                true
        ));

        assertThrows(
                AiServiceException.class,
                () -> analysisService.analyze(request)
        );

        verifyNoInteractions(mcpClient);
    }

    @Test
    void shouldHandleMcpExecutionFailure()
            throws Exception {

        enqueue(toolCall(
                "inspect_network_log",
                Map.of("logContent", ORIGINAL_LOG)
        ));

        when(mcpClient.inspectLog(ORIGINAL_LOG))
                .thenThrow(
                        new IllegalStateException("MCP unavailable")
                );

        assertThrows(
                AiServiceException.class,
                () -> analysisService.analyze(request)
        );

        verify(mcpClient, times(1))
                .inspectLog(ORIGINAL_LOG);
    }

    @Test
    void shouldRejectIncompleteOllamaResponse()
            throws Exception {

        enqueue(ollamaResponse(
                Map.of(
                        "role", "assistant",
                        "content", ""
                ),
                false
        ));

        assertThrows(
                AiServiceException.class,
                () -> analysisService.analyze(request)
        );

        verifyNoInteractions(mcpClient);
    }

    @Test
    void shouldRejectUnexpectedAdditionalToolRequest()
            throws Exception {

        enqueue(toolCall(
                "inspect_network_log",
                Map.of("logContent", ORIGINAL_LOG)
        ));

        // Second Ollama response tries to call another tool.
        enqueue(toolCall(
                "execute_command",
                Map.of("command", "whoami")
        ));

        when(mcpClient.inspectLog(ORIGINAL_LOG))
                .thenReturn("RADIUS_TIMEOUT");

        assertThrows(
                AiServiceException.class,
                () -> analysisService.analyze(request)
        );

        // Only the approved first tool call was executed.
        verify(mcpClient, times(1))
                .inspectLog(ORIGINAL_LOG);
        verifyNoMoreInteractions(mcpClient);
    }

    @Test
    void shouldRejectInvalidFinalDiagnosis()
            throws Exception {

        enqueue(toolCall(
                "inspect_network_log",
                Map.of("logContent", ORIGINAL_LOG)
        ));

        String invalidDiagnosis = mapper.writeValueAsString(
                Map.of(
                        "problemSummary", "",
                        "possibleRootCauses", List.of(),
                        "severity", "UNKNOWN",
                        "recommendedSteps", List.of()
                )
        );

        enqueue(ollamaResponse(
                Map.of(
                        "role", "assistant",
                        "content", invalidDiagnosis
                ),
                true
        ));

        when(mcpClient.inspectLog(ORIGINAL_LOG))
                .thenReturn("RADIUS_TIMEOUT");

        assertThrows(
                AiServiceException.class,
                () -> analysisService.analyze(request)
        );

        verify(mcpClient, times(1))
                .inspectLog(ORIGINAL_LOG);
    }
}
