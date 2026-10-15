
package com.networkagent.service;

import com.networkagent.client.OllamaApiClient;
import com.networkagent.dto.DiagnosisResponse;
import com.networkagent.dto.NetworkLogRequest;

import org.springframework.stereotype.Service;

@Service
public class NetworkAnalysisService {

    private final OllamaApiClient ollamaApiClient;
    private final DiagnosisValidator diagnosisValidator;

    public NetworkAnalysisService(
            OllamaApiClient ollamaApiClient,
            DiagnosisValidator diagnosisValidator
    ) {
        this.ollamaApiClient = ollamaApiClient;
        this.diagnosisValidator = diagnosisValidator;
    }

    public DiagnosisResponse analyzeLog(
            NetworkLogRequest request
    ) {
        DiagnosisResponse diagnosis =
                ollamaApiClient.analyze(request);

        diagnosisValidator.validate(diagnosis);

        return diagnosis;
    }
}
