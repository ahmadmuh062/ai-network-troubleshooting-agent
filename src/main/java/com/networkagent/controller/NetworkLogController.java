
package com.networkagent.controller;

import com.networkagent.dto.NetworkLogRequest;
import com.networkagent.dto.NetworkLogResponse;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class NetworkLogController {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    NetworkLogController.class
            );

    // Simple endpoint to verify server availability.
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {

        return ResponseEntity.ok(
                Map.of("status", "UP")
        );
    }

    // Accept network logs for future AI analysis.
    @PostMapping("/logs/analyze")
    public ResponseEntity<NetworkLogResponse> analyzeLogs(
            @Valid @RequestBody NetworkLogRequest request) {

        // Log metadata without exposing raw log contents.
        logger.info(
                "Network log received: device={}, type={}",
                request.deviceName().replaceAll("[\\r\\n]", "_"),
                request.deviceType().replaceAll("[\\r\\n]", "_")
        );

        // Phase 1 acknowledgment. No AI analysis yet.
        NetworkLogResponse response =
                new NetworkLogResponse(
                        "RECEIVED",
                        request.deviceName(),
                        "Log received successfully. "
                                + "AI analysis is not enabled yet."
                );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }
}
