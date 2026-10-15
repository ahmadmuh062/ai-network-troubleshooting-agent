
package com.networkagent.service;

import com.networkagent.dto.DiagnosisResponse;
import com.networkagent.exception.AiServiceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosisValidatorTest {

    private DiagnosisValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DiagnosisValidator();
    }

    @Test
    void shouldAcceptValidDiagnosis() {

        DiagnosisResponse diagnosis = new DiagnosisResponse(
                "RADIUS authentication timeout",
                List.of("RADIUS server unreachable"),
                "MEDIUM",
                List.of("Check server connectivity")
        );

        assertDoesNotThrow(
                () -> validator.validate(diagnosis)
        );
    }

    @Test
    void shouldRejectMissingSummary() {

        DiagnosisResponse diagnosis = new DiagnosisResponse(
                "",
                List.of("Network failure"),
                "HIGH",
                List.of("Check connectivity")
        );

        assertThrows(
                AiServiceException.class,
                () -> validator.validate(diagnosis)
        );
    }

    @Test
    void shouldRejectInvalidSeverity() {

        DiagnosisResponse diagnosis = new DiagnosisResponse(
                "Network failure",
                List.of("Connectivity issue"),
                "UNKNOWN",
                List.of("Check connectivity")
        );

        assertThrows(
                AiServiceException.class,
                () -> validator.validate(diagnosis)
        );
    }

    @Test
    void shouldRejectEmptyRootCauses() {

        DiagnosisResponse diagnosis = new DiagnosisResponse(
                "Network failure",
                List.of(),
                "HIGH",
                List.of("Check connectivity")
        );

        assertThrows(
                AiServiceException.class,
                () -> validator.validate(diagnosis)
        );
    }

    @Test
    void shouldRejectBlankTroubleshootingStep() {

        DiagnosisResponse diagnosis = new DiagnosisResponse(
                "Network failure",
                List.of("Connectivity issue"),
                "MEDIUM",
                List.of("")
        );

        assertThrows(
                AiServiceException.class,
                () -> validator.validate(diagnosis)
        );
    }

    @Test
    void shouldRejectNullDiagnosis() {

        assertThrows(
                AiServiceException.class,
                () -> validator.validate(null)
        );
    }
}
