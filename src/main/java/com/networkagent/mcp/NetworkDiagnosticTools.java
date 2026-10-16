
package com.networkagent.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class NetworkDiagnosticTools {

    @Tool(
            name = "inspect_network_log",
            description = """
            Inspect a network device log for known failure indicators.
            Read-only analysis. Does not connect to network devices
            or modify any configuration.
            """
    )
    public String inspectNetworkLog(String logContent) {

        if (logContent == null || logContent.isBlank()) {
            return "ERROR: Log content is required";
        }

        if (logContent.length() > 20000) {
            return "ERROR: Log content exceeds the allowed limit";
        }

        String normalized =
                logContent.toLowerCase(Locale.ROOT);

        List<String> findings = new ArrayList<>();

        // RADIUS authentication problems
        if (normalized.contains("radius")
                && (normalized.contains("timeout")
                || normalized.contains("did not respond")
                || normalized.contains("timed out"))) {

            findings.add(
                    "RADIUS_TIMEOUT: Authentication server "
                            + "response timeout detected"
            );
        }

        // DHCP address allocation problems
        if (normalized.contains("dhcp")
                && (normalized.contains("pool exhausted")
                || normalized.contains("no addresses")
                || normalized.contains("no available"))) {

            findings.add(
                    "DHCP_POOL_EXHAUSTED: Possible DHCP "
                            + "address pool exhaustion detected"
            );
        }

        // Physical or logical network interface problems
        if (normalized.contains("link down")
                || normalized.contains("interface down")
                || normalized.contains("changed state to down")) {

            findings.add(
                    "LINK_DOWN: Network interface "
                            + "reported a down state"
            );
        }

        // Network packet loss indicators
        if (normalized.contains("packet loss")
                || normalized.contains("packets dropped")) {

            findings.add(
                    "PACKET_LOSS: Packet loss indicator detected"
            );
        }

        if (findings.isEmpty()) {
            return "NO_KNOWN_PATTERNS: No recognized failure "
                    + "patterns found in the supplied log";
        }

        return String.join("\n", findings);
    }
}
