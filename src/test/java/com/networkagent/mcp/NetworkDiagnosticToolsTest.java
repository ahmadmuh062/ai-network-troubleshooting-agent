
package com.networkagent.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkDiagnosticToolsTest {

    private NetworkDiagnosticTools tools;

    @BeforeEach
    void setUp() {
        tools = new NetworkDiagnosticTools();
    }

    @Test
    void shouldDetectRadiusTimeout() {

        String result = tools.inspectNetworkLog(
                "RADIUS server did not respond. Authentication timeout."
        );

        assertTrue(result.contains("RADIUS_TIMEOUT"));
    }

    @Test
    void shouldDetectDhcpExhaustion() {

        String result = tools.inspectNetworkLog(
                "DHCP address pool exhausted. No addresses available."
        );

        assertTrue(result.contains("DHCP_POOL_EXHAUSTED"));
    }

    @Test
    void shouldDetectLinkDown() {

        String result = tools.inspectNetworkLog(
                "Interface Gi1/0/24 changed state to down."
        );

        assertTrue(result.contains("LINK_DOWN"));
    }

    @Test
    void shouldDetectPacketLoss() {

        String result = tools.inspectNetworkLog(
                "Network packet loss reached 15 percent."
        );

        assertTrue(result.contains("PACKET_LOSS"));
    }

    @Test
    void shouldHandleUnknownPatterns() {

        String result = tools.inspectNetworkLog(
                "Device started successfully."
        );

        assertTrue(result.contains("NO_KNOWN_PATTERNS"));
    }

    @Test
    void shouldRejectEmptyLog() {

        String result = tools.inspectNetworkLog("");

        assertTrue(result.startsWith("ERROR:"));
    }

    @Test
    void shouldRejectOversizedLog() {

        String result = tools.inspectNetworkLog(
                "x".repeat(20001)
        );

        assertTrue(result.startsWith("ERROR:"));
    }

    @Test
    void shouldDetectMultipleProblems() {

        String result = tools.inspectNetworkLog(
                "RADIUS timeout. Interface down."
        );

        assertTrue(result.contains("RADIUS_TIMEOUT"));
        assertTrue(result.contains("LINK_DOWN"));
    }
}
