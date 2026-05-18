package ru.it_spectrum.ai.sonar.mcp.api;

public record Hotspot(
        String key,
        String projectKey,
        String componentKey,
        String componentPath,
        Integer line,
        String message,
        String status,
        String resolution,
        String securityCategory,
        String vulnerabilityProbability,
        String ruleKey,
        String author,
        String assignee,
        String creationDate,
        String updateDate
) {
}
