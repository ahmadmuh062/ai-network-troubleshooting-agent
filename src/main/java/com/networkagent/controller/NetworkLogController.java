
package com.networkagent.controller;

import com.networkagent.dto.DiagnosisResponse;
import com.networkagent.dto.NetworkLogRequest;
import com.networkagent.service.NetworkAnalysisService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class NetworkLogController {

    private final NetworkAnalysisService analysisService;

    public NetworkLogController(
            NetworkAnalysisService analysisService
    ) {
        this.analysisService = analysisService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(
                Map.of("status", "UP")
        );
    }

    @PostMapping("/logs/analyze")
    public ResponseEntity<DiagnosisResponse> analyzeLogs(
            @Valid @RequestBody NetworkLogRequest request
    ) {
        DiagnosisResponse diagnosis =
                analysisService.analyzeLog(request);

        return ResponseEntity.ok(diagnosis);
    }
}
