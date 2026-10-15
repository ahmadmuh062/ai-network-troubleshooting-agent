
package com.networkagent.service;

import com.networkagent.dto.DiagnosisResponse;
import com.networkagent.exception.AiServiceException;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class DiagnosisValidator {

    private static final Set<String> VALID_SEVERITIES =
            Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    public void validate(DiagnosisResponse diagnosis) {

        if (diagnosis == null) {
            throw new AiServiceException(
                    "AI diagnosis is missing"
            );
        }

        if (diagnosis.problemSummary() == null
                || diagnosis.problemSummary().isBlank()) {
            throw new AiServiceException(
                    "AI diagnosis has no problem summary"
            );
        }

        if (!VALID_SEVERITIES.contains(diagnosis.severity())) {
            throw new AiServiceException(
                    "AI diagnosis has an invalid severity"
            );
        }

        validateList(
                diagnosis.possibleRootCauses(),
                "possibleRootCauses"
        );

        validateList(
                diagnosis.recommendedSteps(),
                "recommendedSteps"
        );
    }

    private void validateList(
            List<String> values,
            String fieldName
    ) {
        if (values == null || values.isEmpty()) {
            throw new AiServiceException(
                    "AI diagnosis has no " + fieldName
            );
        }

        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new AiServiceException(
                        "AI diagnosis contains an empty "
                                + fieldName + " entry"
                );
            }
        }
    }
}
