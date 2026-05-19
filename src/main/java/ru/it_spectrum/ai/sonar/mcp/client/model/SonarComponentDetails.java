package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarComponentDetails(
        String key,
        String name,
        String description,
        String qualifier,
        String visibility,
        String analysisDate,
        String version
) {
}
