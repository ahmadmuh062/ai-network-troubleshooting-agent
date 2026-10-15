package com.networkagent.dto;

import java.util.List;

public record DiagnosisResponse(
        String problemSummary,
        List<String> possibleRootCauses,
        String severity,
        List<String> recommendedSteps
) {
}
