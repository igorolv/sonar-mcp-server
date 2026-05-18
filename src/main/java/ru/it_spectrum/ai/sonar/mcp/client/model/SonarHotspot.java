package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarHotspot(
        String key,
        String component,
        String project,
        String securityCategory,
        String vulnerabilityProbability,
        String status,
        String resolution,
        Integer line,
        String message,
        String author,
        String assignee,
        String creationDate,
        String updateDate,
        String ruleKey
) {
}
