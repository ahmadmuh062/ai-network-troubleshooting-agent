
package com.networkagent.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class NetworkMcpClient {

    private final String serverUrl;
    private final String endpoint;

    public NetworkMcpClient(
            @Value("${network.mcp.url}") String serverUrl,
            @Value("${network.mcp.endpoint}") String endpoint
    ) {
        this.serverUrl = serverUrl;
        this.endpoint = endpoint;
    }

    public String inspectLog(String logContent) {

        if (logContent == null
                || logContent.isBlank()
                || logContent.length() > 20000) {
            throw new IllegalArgumentException(
                    "Invalid log content"
            );
        }

        // Connect to the existing localhost MCP server.
        var transport = HttpClientStreamableHttpTransport
                .builder(serverUrl)
                .endpoint(endpoint)
                .build();

        try (McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build()) {

            client.initialize();

            // Only the explicitly approved tool can be invoked.
            McpSchema.CallToolResult result = client.callTool(
                    new McpSchema.CallToolRequest(
                            "inspect_network_log",
                            Map.of("logContent", logContent)
                    )
            );

            if (Boolean.TRUE.equals(result.isError())) {
                throw new IllegalStateException(
                        "MCP diagnostic tool failed"
                );
            }

            return result.content().toString();
        }
    }
}
