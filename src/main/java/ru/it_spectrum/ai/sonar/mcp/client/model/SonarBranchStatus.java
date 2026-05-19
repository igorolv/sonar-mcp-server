package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarBranchStatus(
        String qualityGateStatus,
        Long bugs,
        Long vulnerabilities,
        Long codeSmells
) {
}
