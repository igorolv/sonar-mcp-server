package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarComponent(
        String key,
        String name,
        String longName,
        String qualifier,
        String path,
        String language,
        String project,
        Boolean enabled
) {
}
