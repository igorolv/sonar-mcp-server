package ru.it_spectrum.ai.sonar.mcp.api;

public record HotspotRule(
        String key,
        String name,
        String securityCategory,
        String vulnerabilityProbability,
        String riskDescription,
        String vulnerabilityDescription,
        String fixRecommendations
) {
}
