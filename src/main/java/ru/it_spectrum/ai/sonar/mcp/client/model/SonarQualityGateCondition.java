package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarQualityGateCondition(
        String status,
        String metricKey,
        String comparator,
        String errorThreshold,
        String actualValue,
        Integer periodIndex
) {
}
