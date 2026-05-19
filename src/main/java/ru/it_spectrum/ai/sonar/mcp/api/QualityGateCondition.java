package ru.it_spectrum.ai.sonar.mcp.api;

public record QualityGateCondition(
        String metricKey,
        String comparator,
        String errorThreshold,
        String actualValue,
        String status
) {
}
