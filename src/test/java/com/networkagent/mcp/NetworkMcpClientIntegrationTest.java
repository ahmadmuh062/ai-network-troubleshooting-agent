
package com.networkagent.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

class NetworkMcpClientIntegrationTest {

    @Test
    @EnabledIfEnvironmentVariable(
            named = "RUN_MCP_INTEGRATION",
            matches = "true"
    )
    void shouldCallRealMcpServer() {

        NetworkMcpClient client = new NetworkMcpClient(
                "http://127.0.0.1:8080",
                "/mcp"
        );

        String result = client.inspectLog(
                "RADIUS server did not respond. "
                        + "Authentication timeout."
        );

        assertTrue(result.contains("RADIUS_TIMEOUT"));
    }
}
