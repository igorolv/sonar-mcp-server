package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarBranch(
        String name,
        Boolean isMain,
        String type,
        Boolean excludedFromPurge,
        String analysisDate,
        SonarBranchStatus status
) {
}
