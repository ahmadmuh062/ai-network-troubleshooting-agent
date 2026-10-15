
package com.networkagent.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception
    ) {

        List<String> details = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": "
                                + error.getDefaultMessage()
                )
                .sorted()
                .toList();

        return ResponseEntity.badRequest().body(
                Map.of(
                        "status", 400,
                        "error", "VALIDATION_ERROR",
                        "message", "Invalid network log request",
                        "details", details
                )
        );
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAiFailure(
            AiServiceException exception
    ) {

        // Log the category, not raw logs or model responses.
        logger.warn(
                "AI service failure: {}",
                exception.getClass().getSimpleName()
        );

        return ResponseEntity.status(502).body(
                Map.of(
                        "status", 502,
                        "error", "AI_SERVICE_ERROR",
                        "message",
                        "Network log analysis is temporarily unavailable"
                )
        );
    }
}
