package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarHotspotRule(
        String key,
        String name,
        String securityCategory,
        String vulnerabilityProbability,
        String riskDescription,
        String vulnerabilityDescription,
        String fixRecommendations
) {
}
