
package com.networkagent.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation() {
        return ResponseEntity.badRequest().body(
                Map.of(
                        "status", 400,
                        "error", "VALIDATION_ERROR",
                        "message", "Invalid network log request"
                )
        );
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAiFailure(
            AiServiceException exception
    ) {
        return ResponseEntity.status(502).body(
                Map.of(
                        "status", 502,
                        "error", "AI_SERVICE_ERROR",
                        "message", "Local AI analysis is unavailable"
                )
        );
    }
}
