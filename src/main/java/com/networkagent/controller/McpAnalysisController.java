
package com.networkagent.controller;

import com.networkagent.dto.DiagnosisResponse;
import com.networkagent.dto.NetworkLogRequest;
import com.networkagent.service.McpAnalysisService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logs")
public class McpAnalysisController {

    private final McpAnalysisService mcpAnalysisService;

    // Constructor-based dependency injection
    public McpAnalysisController(
            McpAnalysisService mcpAnalysisService
    ) {
        this.mcpAnalysisService = mcpAnalysisService;
    }

    // Analyze network logs using Ollama + MCP tools
    @PostMapping("/analyze-with-tools")
    public ResponseEntity<DiagnosisResponse> analyzeWithTools(
            @Valid @RequestBody NetworkLogRequest request
    ) {

        DiagnosisResponse diagnosis =
                mcpAnalysisService.analyze(request);

        return ResponseEntity.ok(diagnosis);
    }
}
