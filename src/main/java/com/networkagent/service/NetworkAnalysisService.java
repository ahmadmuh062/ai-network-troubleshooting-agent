
package com.networkagent.service;

import com.networkagent.client.OllamaApiClient;
import com.networkagent.dto.DiagnosisResponse;
import com.networkagent.dto.NetworkLogRequest;

import org.springframework.stereotype.Service;

@Service
public class NetworkAnalysisService {

    private final OllamaApiClient ollamaApiClient;

    public NetworkAnalysisService(
            OllamaApiClient ollamaApiClient
    ) {
        this.ollamaApiClient = ollamaApiClient;
    }

    public DiagnosisResponse analyzeLog(
            NetworkLogRequest request
    ) {
        return ollamaApiClient.analyze(request);
    }
}
