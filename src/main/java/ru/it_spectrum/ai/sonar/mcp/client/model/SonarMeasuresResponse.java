package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarMeasuresResponse(
        ComponentWithMeasures component
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ComponentWithMeasures(
            String key,
            String name,
            String qualifier,
            List<SonarMeasure> measures
    ) {
    }
}
