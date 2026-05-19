package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarQualityGateStatusResponse(
        ProjectStatus projectStatus
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectStatus(
            String status,
            List<SonarQualityGateCondition> conditions
    ) {
    }
}
